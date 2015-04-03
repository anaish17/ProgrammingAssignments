import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;


/**
 * DnsClient takes as input the IP address of a root DNS server and a domain name,
 * queries through the DNS server and intermediate servers to obtain the IP 
 * address of the given domain name. There is a timeout of 5 seconds, if a packet
 * is not sent within that time, it will be resent.
 * 
 * @author Anais Hernandez
 *         PID: 3884585
 *         CNT 4713 Spring 2015
 *         Project 2: DNS Resolver
 * 
 */
public class DnsClient {
   

   public static void main (String args[]) throws IOException
   {
      System.out.println("--------------------");
      
      String hostname=""; 
      String ip="";
      
      if(args.length != 2)
      {
         System.out.println("Invalid Arguments. Include IP address of Root DNS Server "
                              + "followed by a domain name.");
         System.exit(0);
      }
      else
      {
        ip = args[0];         //get ip and hostname inputted by user
        hostname = args[1];
      }
      
      //String hostname="cs.fiu.edu";
      //String ip="192.228.79.201";
      
      InetAddress ROOT_IP_ADDR = InetAddress.getByName(ip);  //address for root dns
      Query p = new Query(hostname, ROOT_IP_ADDR);         //query object
      int PORT_NUM = 53;       //port for dns
      
      boolean found=false;    //if an answer is found
      
      while(found==false)     //while no answer has been found
      {
         try
         {
            System.out.println("DNS Server to Query: " + p.ipaddr.getHostAddress());

            int headerSize = 12; //12 bytes in header

            //Make Header for packet
            byte[] msg = new byte[18+p.hostname.length()];    //12 for header, 6 for question, and hostname
            msg = p.addHeader(msg, 1);

            int currentPosition = headerSize;     //current position of byte array is 12(after header)

            //create question section for packet
            msg = p.addQuestion(msg, currentPosition);


            //CREATE SOCKET
            DatagramSocket socket = new DatagramSocket();
            socket.connect(p.ipaddr, PORT_NUM);


            //SEND PACKET
            System.out.println("Sending Packet...");
            DatagramPacket packet = new DatagramPacket(msg, msg.length, p.ipaddr, PORT_NUM);
            socket.send(packet);
            System.out.println("Packet has been sent.");


            //RECIEVE PACKET
            byte[] response = new byte[1024];      //holds bytes to be recieved
            socket.setSoTimeout(5000);             //SET TIME OUT TO 5 SECONDS
            
            while(true)
            {
               DatagramPacket incoming = new DatagramPacket(response, response.length);
               try
               {
                  socket.receive(incoming);     //recieve incoming packet
                  response = incoming.getData();
               }
               catch (IOException e)   //RESEND packet
               {
                  socket.send(packet);
                  System.out.println("Packet has been resent.");
               }

               //printing incoming byte array
              /*for(int i =0; i<response.length; i++)
               {
                  byte a =response[i];
                  System.out.println("Byte#" + i + " "+ (a < 0 ? 256 + a : a));  //print as unsigned
               }*/
               
               
               //CHECK RESPONSE
               String[] nameServers = p.checkResponseHeader(response);
               found = p.answerFound;     //Check if an answer has been found
               
               if(found == false)   //keep querying if not found yet
               {   //make new query object
                  InetAddress new_ip = InetAddress.getByName(p.nameServers[0]);
                  p = new Query(p.hostname, new_ip);
               }
               
               System.out.println("\n\n");
               socket.close();

            }

         }
         catch (SocketException e)
         { 
            //socket closed          
         }
         catch (SocketTimeoutException e)
         {
            //no reply recieved, timer ran out
         }
         catch (IOException e)
         {
            System.out.println(e);
         }
      
      }
      

   }
   
}
