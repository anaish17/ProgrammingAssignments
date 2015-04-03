import java.net.InetAddress;

/**
 * Formats information for query to send and deciphers information from
 * incoming reply.
 * 
 * @author Anais Hernandez
 */
public class Query
   {
      InetAddress ipaddr;              //holds ipaddress to query to
      boolean answerFound=false;       //if answer is found
      String hostname;                 //holds domain name to look for
      String[] name;                   //holds all names in authoritative section
      int nIndex=0;                    //index for name array
      String[] answer;                 //holds all ip addresses in answer section
      int aIndex=0;                    //index for answer array
      String[] nameServers;            //holds all name servers in auth section
      int nsIndex=0;                   //index for nameServer array
      String[] additional;             //holds all ip addresses in additional section
      int addindex=0;                  //index for additional array
      
      /**
       * Contains domain name to look for and IP address to query to.
       * @param host
       * @param ipa 
       */
      public Query (String host, InetAddress ipa)
      {
         hostname = host;
         ipaddr=ipa;
      }
      
      
      /**
       * Make the header field for the packet.
       * @param buffer header byte array
       * @param recdesired 1 if recursion is desired else 0
       * @return byte array with all fields of header
       */
      public byte[] addHeader(byte[] buffer, int recdesired)
      {
         buffer[0]= 0;           //ID
         buffer[1]= 0;           //ID - 16 bits
         buffer[2]= (byte)recdesired;     //flag
         buffer[3]= 0;                    //flag
         buffer[4]= 0;           //qdcount
         buffer[5]= (byte)1;     //qdcount - 16 bits
         buffer[6]= 0;           //ancount
         buffer[7]= 0;           //ancount - 16 bits
         buffer[8]= 0;           //nscount
         buffer[9]= 0;           //nscount - 16 bits
         buffer[10]= 0;          //arcount
         buffer[11]= 0;          //arcount - 16 bits
         return buffer;
      }

      
      /**
       * Add question section (query name, qtype, qclass) to byte array.
       * @param buffer byte array to send
       * @param currentPos current index
       * @return 
       */
      public byte[] addQuestion(byte[] buffer, int currentPos)
      {
         String[] arr = hostname.split("\\.");
         for(int i = 0; i<arr.length; i++)
         {
            buffer[currentPos++] = (byte)arr[i].length();   //add label of length
            for(int j = 0; j < arr[i].length(); j++)
            {
               buffer[currentPos++]=(byte)arr[i].charAt(j);    //add sequence of chars
            }
         }
         buffer[currentPos++] = 0;        //endofqueryname - null
         buffer[currentPos++] = 0;        //qtype
         buffer[currentPos++] = (byte)1;  //qtype - A
         buffer[currentPos++] = 0;        //qclass
         buffer[currentPos++] = (byte)1;  //qclass - Internet Class
         return buffer;
      }
      
      
      /**
       * Look up header information to check response.
       * @param response buffer received
       * @return array of ip addresses of name servers
       */
      public String[] checkResponseHeader(byte[] response)
      {
         byte ID = response[0];     //ID - 16 bits
         byte ID2 = response[1];
         byte flag1 = response[2];  //get flags - 16 bits
         byte flag2 = response[3];
         
         if(flag2 != (byte)0)       //CHECK RCODE for error
         {   
            System.out.println(checkError(flag2));
         }
         
         byte qcount = response[5];       //number of entries in question
         byte anscount = response[7];     //answer section count
         byte authcount = response[9];    //authority section count
         byte addcount = response[11];    //additional section count
         
         
         System.out.println("Reply Recieved. Content overview: ");
         System.out.println(qcount + " Question");
         System.out.println(anscount + " Answer(s).");
         System.out.println(authcount + " Intermediate Name Server(s).");
         System.out.println(addcount + " Additional Information Record(s).");
         
         //Check if a blank reply
         if(anscount ==0 && authcount==0 && addcount==0)
         {   
            System.out.println("Nothing was recieved");
            System.exit(0);
         }
         
         //initialize size of arrays for each section
         answer = new String[anscount];
         nameServers = new String[authcount];
         name = new String[authcount];
         additional = new String[addcount];
         
         int offset = 11+hostname.length()+2;    //end of qname
         offset++;
         
         byte qtype = response[++offset];
         offset++;
         byte qclass = response[++offset]; 
         
         //Check answer section
         if(anscount != 0)
         {
            offset = checkAnswerSection(response, offset, anscount);
            //offset++;
         }

         //Check authoritative section
         if(authcount != 0)
         {
            offset = checkAuthSection(response, offset, authcount);
            offset++;
         }
         
         
         //check additional information section
         if(addcount !=0)
         {
            offset = checkAdditionalSection(response, offset, addcount);
            offset++;
         }
         
         return additional;
      }
      
      
      /**
       * Read answer section.
       * @param response byte array of incoming reply
       * @param offset current position
       * @param anscount number of answers in section
       * @return offset current position
       */
      public int checkAnswerSection(byte[] response, int offset, int anscount)
      {
         System.out.println("----Answer Section----");
         answerFound = true;
         
            for(int i = 0; i < anscount; i++)
            { 
               String ip="";
             //check if name is a pointer
             if(response[++offset] < 0)    //first two bits are 11 (pointer with offset)
            {
               skipToName(response, response[++offset]);
            } 
            offset +=2;
            byte type = response[offset++];
            byte classcode = response[++offset];
            byte ttl0 = response[++offset];  //TIME TO LEAVE TTL - 32 bits
            byte ttl1 = response[++offset];
            byte ttl2 = response[++offset];
            byte ttl3 = response[++offset];  
            offset+=2;
            byte rlength = response[offset++];
            for(int j = 0; j < rlength; j++)
            {
               if(type != 1)
               {
                  offset+=rlength; break;
               }
               byte curr = response[offset++];
               ip += (curr < 0 ? 256 + curr : curr);     //get unsigned byte 
               
               if(j+1!=rlength)     //if not end of sequence of chars, add "."
                  ip +=".";

            }
            System.out.println("Name: " + hostname + "\t" + "IP: " + ip);
         }
         
         
         return offset;
     }
      
      
     /**
      * Read Authoritative section.
      * @param response byte array of incoming reply
      * @param offset current position
      * @param authcount number of name servers in section
      * @return offset current position
      */
     public int checkAuthSection(byte[] response, int offset, int authcount)
     {
         System.out.println("----Authoritative Section----");
         for(int i = 0; i < authcount; i++)
         { 
             //check if name is a pointer
             if(response[++offset] < 0)    //first two bits are 11 (pointer with offset)
            {
               skipToName(response, response[++offset]);
            } 
            offset +=2;
            byte type = response[offset++];
            byte classcode = response[++offset];
            byte ttl0 = response[++offset];  //TIME TO LEAVE TTL - 32 bits
            byte ttl1 = response[++offset];
            byte ttl2 = response[++offset];
            byte ttl3 = response[++offset];  
            offset+=2;
            byte rlength = response[offset++];
            offset = convertHostname(response, offset, rlength-1);
         }
            
         for(int i = 0; i < name.length; i++)
         {
            System.out.println("Name: " + name[i] + "\t" + "Name Server: " + 
                                          nameServers[i]);
         }
         
         return offset;
     }
      
     
     /**
      * Read Additional Information section.
      * @param response byte array of incoming reply
      * @param offset current position
      * @param addcount number of names in add section
      * @return offset current position
      */
      public int checkAdditionalSection(byte[] response, int offset, int addcount)
      {
         System.out.println("----Additional Information Section----");
         for(int i =0; i < addcount; i++)
         {
            String ip ="";
            String nameServ="";
            if(response[offset] < 0)    //first two bits are 11 (pointer with offset)
            {
               byte c = response[++offset];
               nameServ = checkCorrespondingNameServer((c < 0 ? 256 + c : c), response);
            } 
            
            offset +=2;
            byte type2 = response[offset++];
            byte class2 =response[++offset];
            offset+=5; //skip ttl
            byte rlength = response[++offset];
         
            for(int j = 0; j < rlength; j++)
            {
               if(type2 != 1)    //some may be type 28 (ipv6), if so skip
               {
                  offset+=rlength; break;
               }
               byte curr = response[++offset];
               ip += (curr < 0 ? 256 + curr : curr);
               if(j+1!=rlength)
                  ip +=".";
            }
            additional[addindex++] = ip;
            System.out.println("Name Server: " + nameServ +"\tIP Address: " + ip);
            offset++;
         }
         return offset;
     }
     
     
      /**
       * Check which name server is being referred to in Additional Section.
       * @param index - index to jump to
       * @param response - byte array
       * @return string of corresponding name server
       */
      public String checkCorrespondingNameServer(int index, byte[] response)
      {
        int numOfChar = response[index++];
         String host = "";
         for(int i = 1; i <= numOfChar; i++)
         {
            host += (char)response[index++];
         }
         
         for(int i = 0; i < nameServers.length; i++)
         {
            if(nameServers[i].startsWith(host))
               return nameServers[i].toString();
         }
         return "";
         
      }
      
      
      /**
       * Check name section of reply.
       * @param response byte array of reply
       * @param offset current position
       */
      public void skipToName(byte[] response, int offset)
      {
         StringBuilder sb = new StringBuilder();
         int numberOfCharacters = 0;
         
         while(response[offset] != (byte)0)     //keep reading until end of data
         {
            numberOfCharacters = response[offset++];
            for(int i = 1 ; i <= numberOfCharacters; i++)
            {
               sb.append((char)response[offset]);
               offset++;
            }
            if(response[offset] != (byte)0)     //end of sequence of chars, add period
               sb.append('.');
            
         }
         //System.out.println("Name: " + sb.toString());
         if(name.length > nIndex )name[nIndex++] = sb.toString();
      }
      
      
      /**
       * Get name server from a previous position.
       * 
       * @param response buffer of incoming reply
       * @param offset position to go back to
       * @param sb string of name server
       */
      public void skipToNameServer(byte[] response, int offset, StringBuilder sb)
      {
         int numberOfCharacters = 0;
         
         while(response[offset] != (byte)0)
         {
            numberOfCharacters = response[offset++];
            for(int i = 1 ; i <= numberOfCharacters; i++)
            {
               sb.append((char)response[offset]);
               offset++;
            }
            if(response[offset] != (byte)0)     //end of sequence of chars, add period
               sb.append('.');
         }
         nameServers[nsIndex++] = sb.toString();
      }
      
      
      /**
       * Get name server from authoritative section.
       * 
       * @param response buffer of incoming reply
       * @param offset current position
      * @param rlength length of auth section data
       * @return current index of buffer
       */
      public int convertHostname(byte[] response, int offset, int rlength)
      {
         int numberOfCharacters =0;
         StringBuilder sb = new StringBuilder();
         while(response[offset] != (byte)0 || rlength <= 0)
         {
            if(response[offset] < 0)      //bit starts with 11 - points to a previous name server
            {
               offset++;
               byte c = response[offset];
               skipToNameServer(response, (c < 0 ? 256 + c : c), sb);
               return offset;
            }
            numberOfCharacters = response[offset++];
            for(int i = 1 ; i <= numberOfCharacters; i++)
            {
               sb.append((char)response[offset]);
               offset++;
            }
            if(response[offset] != (byte)0)     //end of sequence of chars, add period
               sb.append('.');
            rlength--;
         }
         nameServers[nsIndex++] = sb.toString();
         
         return offset;
      }
      
      
      /**
       * Check RCODE for error messages, if any.
       * 
       * @param flagerror byte received as RCODE
       * @return error message as a string
       */
      public String checkError(byte flagerror)
      {
         if(flagerror==1)
            return flagerror + " Format error - The name server was unable to interpret the query.";

         else if(flagerror==2)
            return flagerror + " Server failure - The name server was unable to process this query "
                + "due to a problem with the name server..";

         else if(flagerror==3)
            return flagerror + " Name Error - Meaningful only for responses from an authoritative "
                + "name server, this code signifies that the domain name referenced "
                + "in the query does not exist.";

         else if(flagerror==4)
            return flagerror + " Not Implemented - The name server does not support the requested "
                + "kind of query.";

         else if(flagerror==5)
            return flagerror + " Error: Refused - The name server refuses to perform the specified operation "
                + "for policy reasons.";

         else return flagerror + " RCODE is 6-15 and is reserved for future use.";
      
      }
      
   }
