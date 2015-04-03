import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * MyFTPServer is a multi-threaded simplified version of an FTP server that 
 * takes one parameter (the port number) and has the implementation of the 
 * following commands: user, password, mkdir - make directory, cd - change directory, 
 * pwd- print working directory, get- file from server to client, 
 * put- file from client to server, delete- remove a file, and quit.
 * 
 * @author Anais Hernandez
 * 
 */
public class MyFtpServer implements Runnable 
{
   Socket clientSocket;
   String user;
   boolean isLoggedIn = false;
   String rootDirectory;
   String currentDirectory;

   
   /**
    * Constructor for FTP SERVER
    * @param s client's socket
    */
   MyFtpServer(Socket s) 
   {
      clientSocket = s;
   } 


   /**
    * Process user command. 
    * @param arguments
    * @param out 
    */
   public void user(String arguments[] , PrintWriter out)
   {
        if(arguments.length != 2)  //invalid number of arguments
        {
            print("501 Syntax error in parameters or arguments.", out);
        }
        else        //user provided user name
        {
            print ("331 User name okay, need password.", out);
            user = arguments[1];
        }
                     
   }
   
   
   /**
    * Process password command. User name and password must be the same.
    * @param arguments
    * @param out 
    */
   public void password(String arguments[], PrintWriter out)
   {
        if(arguments.length != 2)     //invalid number of arguments
        {
            print("501 Syntax error in parameters or arguments.", out);
        }
        else 
        {
            if(this.user.equals(arguments[1]))  //log in if user name equals password
            {
                print("230 User logged in, proceed.", out);
                this.isLoggedIn = true;
            }
            else        //user name and password different
            {
                print("530 Login incorrect.", out);
            }
        }      
   }
   
   
   /**
    * Make a new directory.
    * @param arguments
    * @param out 
    */
   public void makeDirectory(String arguments[], PrintWriter out)
   {
      if (arguments.length != 2)    //invalid arguments/parameters
         print("501 Syntax error in parameters or arguments.", out);
      
      else              //make directory
      {
         boolean isSuccessful = new File(arguments[1]).mkdir();
         if(isSuccessful != true)
         {
            print("550 Create directory operation failed.", out);
         }
         else { print("257 Pathname created.", out);}
      }      
   }
   
   
   /**
    * Change working directory
    * @param arguments
    * @param out 
    */
   public void changeDirectory (String arguments[], PrintWriter out)
   {   
        String path;
       
        if(currentDirectory.endsWith("\\"))          //make sure theres only one '\'
        {
            if(arguments[1].startsWith("\\"))
                path = currentDirectory.substring(0, currentDirectory.length()-1) + arguments[1];
            else
                path = currentDirectory + arguments[1];
        }
        else
        {
            if(arguments[1].startsWith("\\"))
                path = currentDirectory + arguments[1];
            else
                path = currentDirectory + "\\" + arguments[1];
        }
       
       File next = new File(path);
       
       if(next.isDirectory() && next.exists())    //if its a directory
       {
           currentDirectory = path;               //update current path
           System.setProperty("user.dir", path);    //change path
           print("250 directory changed", out);
       }
       else             //not a directory
       {
           print("550 Path does not exist." ,out);
       }
   }
   
   
   /**
    * Prints path of the working directory.
    * @param arguments
    * @param out 
    */
   public void printPath(String arguments[], PrintWriter out)
   {
        if(arguments.length != 1) 
            print("501 Syntax error in parameters or arguments.", out);
        else
        {
            print(currentDirectory.replace(rootDirectory, "~") ,out);
        }
   }
   
   
   /**
    * Send a file from server to client
    * @param arguments
    * @param out 
     * @param input 
     * @throws java.io.FileNotFoundException 
     * @throws java.net.UnknownHostException 
     * @throws java.io.IOException 
    */
   public void transfer(String arguments[], PrintWriter out, BufferedReader input) 
           throws FileNotFoundException, UnknownHostException, IOException
   {
       //EPRT |2|1080::8:800:200C:417A|5282|
       //|2|::1|49779|
       String commands = arguments[1];
       System.out.println(arguments[1]);
       
       String seperate [] = commands.split("\\|");      //seperate af, addr, & port
       
       String a_f = seperate[1];
       InetAddress networkAddr = InetAddress.getByName(seperate[2]);
       int port = Integer.parseInt(seperate[3]);
       
       
       String name[] = input.readLine().split(" ");      //RETR - file name is returned
       String fileName = name[1];
       System.out.println(fileName);
       File fileToSend = new File(fileName);
       
       if(name[0].compareTo("RETR") == 0 )    //get file to client
       {
           if(fileToSend.exists())      //if file exists, transfer file with new connection
           {
               Socket dataConnection = new Socket(networkAddr, port);
               print("150 File status okay; about to open data connection.", out);
               get(dataConnection, fileToSend, out);
           }
           else
           {
               print("550 Requested action not taken. File Not Found.", out);
           }
       }
       else if(name[0].compareTo("STOR") == 0)      //store file on server
       {
               Socket dataConnection = new Socket(networkAddr, port);
               print("150 File status okay; about to open data connection.", out);
               put(dataConnection, fileToSend, out);
       }

       
   }
   
   
   /**
    * Transfer a file from server to client via data connection socket
    * @param dataSock
    * @param fileToSend
    * @param out
    * @throws FileNotFoundException
    * @throws IOException 
    */
   public void get(Socket dataSock, File fileToSend, PrintWriter out) 
           throws FileNotFoundException, IOException
   {
        int bytesRead;
        int index;
        OutputStream outStream = null;
        FileInputStream fileIn = null;
        BufferedInputStream buffer = null;

        try 
        {
          // Send File
            print("125 Data connection already open; transfer starting.", out);
            
            byte [] bytearray  = new byte [(int)fileToSend.length()];
            
            fileIn = new FileInputStream(fileToSend);
            buffer = new BufferedInputStream(fileIn);
            buffer.read(bytearray,0,bytearray.length);
            outStream = dataSock.getOutputStream();
            
            System.out.println("Sending " + fileToSend + " : " + bytearray.length + " bytes)");
            
            outStream.write(bytearray,0,bytearray.length);
            
            outStream.flush();
            
            System.out.println("Done.");
        }
        finally 
        {
            if (buffer != null) buffer.close();
            if (outStream != null) outStream.close();
            if (dataSock!=null) dataSock.close();
            else {print("425 Can't open data connection.", out);}
        }

   }
   
   
   /**
    * Receive a file from client.
     * @param dataSock
     * @param fileToSend
    * @param out 
    */
   public void put (Socket dataSock, File fileToSend, PrintWriter out) throws IOException
   {
        int index = 0;
        FileOutputStream fileOut = null;
        BufferedOutputStream buffer = null;
        int bytesRead = 0;
        
        try 
        {
          //Recieve file
            print("125 Data connection already open; transfer starting.", out);
            fileOut = new FileOutputStream(rootDirectory + "\\" +fileToSend);
            buffer = new BufferedOutputStream(fileOut);
            InputStream in = dataSock.getInputStream();
            
            while ((index= in.read()) != -1)
            {
                buffer.write(index);
                bytesRead++;
            }
            
            System.out.println("File " + fileToSend + " downloaded: " + bytesRead + " bytes read");
            System.out.println("Done.");
            //print("226 Closing data connection. Requested file action successful", out);
        }
        finally 
        {
            if (buffer != null) buffer.close();
            if (fileOut != null) fileOut.close();
            if (dataSock!=null) dataSock.close();
            else {print("425 Can't open data connection.", out);}
        }
        
   }
   
   /**
    * Deletes a file from server
    * @param arguments
    * @param out 
    */
   public void delete(String arguments[], PrintWriter out)
   {
        if(arguments.length != 2)
            print ("501 Syntax error in parameters or arguments.", out);
        else
        {
            File file = new File(arguments[1]);
            boolean isDeleted = file.delete();
            if (isDeleted == false || file.isFile())
            {
                print ("550 Requested action not taken. File unavailable.", out);
            }
            else print("250 Requested file action okay, completed.", out);
      }
   }
   
   
   /**
    * Print string to client
    * @param a message to send
    * @param out print writer object
    */
   public void print (String a, PrintWriter out)
   {
      out.println(a);
   }
   
   
   
   
   /**
    * MAIN CLASS- establishes a connection
    * @param args
    * @throws IOException 
    */
   public static void main(String[] args) throws IOException
   {
      int portNumber = 0;
      if(args.length != 1)          //INVALID ARGUMENTS- EXIT PROGRAM
      {   
         System.out.println("Invalid arguments. One parameter for port number.");
         System.exit(0);
      }
      else                          //Get Port Number
      {
          portNumber = Integer.parseInt(args[0]);
      }
      
      //create server socket with port number
      ServerSocket server = new ServerSocket(portNumber);
      
      try
      {
         while(true)
         {
            System.out.println("Waiting for connection...");
            Socket socket = server.accept();
            System.out.println("Connection was accepted.");
            
            MyFtpServer client = new MyFtpServer(socket);    //new socket
            
            client.rootDirectory = System.getProperty("user.dir");   //set current as root
            client.currentDirectory = client.rootDirectory;
            
            Thread t = new Thread(client);      //new thread
            t.start();

         }
      }
      finally
      {
            server.close();      //close connection
      }
        
   }  //END OF MAIN

   
   /**
    * Receives client's input and checks which command was given and performs
    * the required actions for the command.
    * 
    */  
   @Override
   public void run()
   {  
      PrintWriter output = null;   
      try
      {
         output = new PrintWriter(clientSocket.getOutputStream(), true);    
         BufferedReader input = new BufferedReader(new InputStreamReader
                                                       (clientSocket.getInputStream()));
         output.println("220 Service ready for new user.");
         
         try
         {
            String answer = "";
            String arguments[] = null;

            answer = input.readLine();    //read client input
         
            //If client entered a command
            while(answer != null)
            {
               //split client command and arguments  
               
               arguments = answer.split(" ");           
               
               System.out.println("Command Entered:" + arguments[0]);
               
               
               //Check if CLIENT QUITS - close connection
               if(arguments[0].compareTo("QUIT") == 0)
               {
                    this.clientSocket.close();   //close connection
                    System.out.println("Client disconnected.");
                    break;
               }
               
               
               //CHECK COMMAND IF LOGGED IN OR LOGGING IN AND PROCESS IT.
               else  if(isLoggedIn == true || arguments[0].compareTo("USER") == 0 
                                            || arguments[0].compareTo("PASS")==0)   
               {
                  if (arguments[0].compareTo("USER") == 0)
                     user(arguments, output);

                      else if (arguments[0].compareTo("PASS") == 0)
                         password(arguments, output);

                      else if (arguments[0].compareTo("XMKD") == 0)
                         makeDirectory(arguments, output);

                      else if (arguments[0].compareTo("CWD") == 0)
                         changeDirectory(arguments, output);

                      else if (arguments[0].compareTo("XPWD") == 0)
                         printPath(arguments, output);

                      else if (arguments[0].compareTo("EPRT") == 0)
                      {
                         if(arguments.length == 2)
                         {
                             print("200 Command OK.", output);
                            transfer(arguments, output, input);
                         }
                         else
                         {
                            print("500 Syntax error, command unrecognized.", output);
                         }
                      }
                      
                      else if(arguments[0].compareTo("DELE") == 0)
                            delete(arguments, output);

                      else                             //INVALID COMMAND
                         print("500 Syntax error, command unrecognized.", output);
               }
               else
               {
                   output.println("530 Not logged in. Can't proceed.");
               }
                  
               answer = input.readLine();    //get new input
            }
         }
         finally
         {
             if (clientSocket != null)
                clientSocket.close();         //close client socket
         }
      }
      catch (IOException ex) 
      {
            Logger.getLogger(MyFtpServer.class.getName()).log(Level.SEVERE, null, ex);
      }            
      finally
      {
          if (output != null)
            output.close();
      }
      
   }  //END OF run 
    
}  //END OF CLASS
