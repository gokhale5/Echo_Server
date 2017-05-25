import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;


public class Web {
	
	public static void main(String[] args){
	
		/**Makes sure that an integer value between 1025 and 65535 is given
		   Based on the min and max ports available
		*/
		int	portNum = Integer.parseInt(args[0]);	
		ServerSocket echo = init(portNum);
		/**
		 * While the program is running continuously will try and find new incoming connections
		 */
		while(true){
			go(echo,portNum);
		}			
	}
	//Tries to connect to a socket given the users Port number. On Error shuts down the code
	public static ServerSocket init(int portNum){
		ServerSocket echoServer;
		InetSocketAddress inetSocketAddress;		
		try{
			echoServer = new ServerSocket();
			//inetSocketAddress = new InetSocketAddress(InetAddress.getAllByName("127.0.0.1")[0], portNum);
			inetSocketAddress = new InetSocketAddress(InetAddress.getAllByName("128.235.208.225")[0], portNum);
			echoServer.bind(inetSocketAddress);
			
			return echoServer;
		}catch(IOException e){
			//Shuts down the code if there are any issues starting the socket
			System.err.println("Issue Starting Server Socket");
			System.exit(1);
			return null;
		}
	}
	
	public static void go(ServerSocket ss, int portNum){
		InputStream input = null;
		OutputStream output = null;

		//Max size of the information coming in is 65535 program will throw and exception and close the connection
		byte[] b = new byte[65535];
		Socket socket = null;
		int numbytes = 0,bytesread;
		
		try{
			socket = ss.accept();
			input = socket.getInputStream();
			output = socket.getOutputStream();
			
			/**
			 * Program reads in 12 bytes at a time until the stream is empty
			 * On last read bytesread will either equal -1 or < 12. -1 is received
			 * when the stream is empty,  < 12 if there are less than 12 bytes to be read
			 */
			while((bytesread = input.read(b, numbytes, 8)) == 8){
				numbytes += bytesread;
			}
			b = ShortenArray(b);
			
			byte[] Port = ("PORT	= " + portNum).getBytes();
			byte[] fullURL = parse(b);
			byte[] Host = dnslookup(fullURL);

			byte[] path = getPath(fullURL);
			byte[] start = "<html><body>".getBytes();
			byte[] end = "</body></html>".getBytes();
				
			output.write(start);
				
			output.write(addInformation("black", addLineBreaks(b)));
			output.write(addInformation("red", Host));
			output.write(addInformation("red", Port));
			output.write(addInformation("red", path));
				
			output.write(end);
			
		}catch(Exception e){
			e.printStackTrace();
			return;
		}
		try{
			if(input != null){input.close();}
		}catch(IOException e){e.printStackTrace();}
		try{ 
			if(output != null){output.close();}
		}catch(IOException e){e.printStackTrace();}
		try{
			if(socket != null){socket.close();}
		}catch(IOException e){e.printStackTrace();}
}
	public static byte[] ShortenArray(byte[] fullURL) {
		int end = 0;
        for(int x = 0; x < fullURL.length;x++){
            if(fullURL[x] == 0){
                end = x;
                break;
            }
        }
        byte[] full = new byte[end-4];
        System.arraycopy(fullURL, 0, full, 0, end-4);
		return full;
	}
	//Adds HTML tags based on what color the output text needs to be
	public static byte[] addInformation(String color, byte[] byteArray){
		byte[] pend = "</font></p>".getBytes();
		byte[] Complete,pstart;
		
		pstart = color.equals("black") ? "<p><font color=\"black\">".getBytes() : "<p><font color=\"red\">".getBytes();

		Complete = new byte[pstart.length + byteArray.length + pend.length];
		
		System.arraycopy(pstart, 0, Complete, 0, pstart.length);
		System.arraycopy(byteArray, 0, Complete, pstart.length, byteArray.length);
		System.arraycopy(pend, 0, Complete, pstart.length + byteArray.length, pend.length);
		
		return Complete;
	}
	/**
	 * Parses array looking for the URL
	 * The URL is contained on the first line and resembles GET_URL_
	 * Function finds the first space character (32) and copies everything
	 * until the next space character (32) is found
	 */
	public static byte[] parse(byte[] full){
		int start = 0, end = 0;
		
		for(int x = 0; x < full.length; x++){
			if(full[x] == 32){
				if(start == 0){
					start = x + 1;
				}else{
					end = x;
					break;
				}
			}	
		}
		byte[] URL = new byte[end-start+1];
		
		for(int x = start; x < end; x++){
			URL[x-start] = full[x];
		}
		return URL;
	}
	
	/**
	 * Needs to replace line breaks from ASCII to ones recognized by HTML
	 * Current byte array has end lines represented by Carrier Returns + New Line Character
	 * Also as \r\n or (13)(10). Replaces those with <br>
	 */
	public static byte[] addLineBreaks(byte[] full){
		int instances = 0;		
		//Determines how many carrier returns are in the byte array
		for(int x = 0; x < full.length; x++){
			if(full[x] == 13){
				instances++;
			}
		}
		int current = 0;
		int newVal = 0;
		
		//Need to replace \r\n with <br> or 2 characters with 4. 
		//Adds 2*instances to current length
		byte[] newArray = new byte[full.length + (2*instances)];
		
		while(current < full.length){
			if(full[current] == 13){
				newArray[newVal] = 60;
				newArray[newVal + 1] = 66;
				newArray[newVal + 2] = 82;
				newArray[newVal + 3] = 62;
				newVal += 4;
				current++;
			}else{
				newArray[newVal] = full[current];
				newVal++;
			}
			current++;
		}
		return newArray;
	}
	/**
	 * Determines the IP Address and puts it in the format needed. 
	 * Get All By Name Outputs HOST/IP_ADDRESS
	 * Need to replace with HOSTIP = HOST (IPADDRESS)
	 */
	public static byte[] dnslookup(byte[] URL){
		URL url = null;
		InetAddress[] address;
		byte[] hostArray;
		byte[] hostOutput = null;
		byte[] beg = "HOSTIP\t= ".getBytes();
		
		try{
			url = new URL(new String(URL));
			address = InetAddress.getAllByName(url.getHost());			
			hostArray = address[0].toString().getBytes();
			int start = 0;
			//check for /
			for(int x = 0; x < hostArray.length;x++){
				if(hostArray[x] == 47){
					start = x;
					break;
				}
			}
			hostOutput = new byte[hostArray.length + 2 + 9];
			
			for(int x = 0; x < beg.length; x++){
				hostOutput[x] = beg[x];
			}					
			
			for(int y = 0; y < start; y++){
				hostOutput[y + 9] = hostArray[y];
			}
			
			hostOutput[start + 1 + 9] = 32; //_
			hostOutput[start + 2 + 9] = 40; //(

			for(int y = start + 1; y < hostArray.length; y++){
				hostOutput[y + 2 + 9] = hostArray[y];
			}
			hostOutput[hostOutput.length - 1] = 41; //)
			return hostOutput;
		}catch(Exception e){
			//Add domain does not exist to exception
			hostOutput = (url.getHost() + " (Domain does not exist!)").getBytes();	
			return hostOutput;
		}
	}
	public static byte[] getPath(byte[] full){
		try{
			URL url = new URL(new String(full));
			byte[] path = new byte[url.getPath().getBytes().length + 7];
			byte[] beg = "PATH\t= ".getBytes();
			byte temp[] = url.getPath().getBytes();
			
			for(int x = 0; x < beg.length; x++){
				path[x] = beg[x];
			}

			for(int x = 0; x < temp.length; x++){
				path[x + 7] = temp[x];
			}
			return path;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
}
