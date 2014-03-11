import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class CSNode extends Thread{
	String fileName = "//people//cs//t//txc121730//logs.txt";
	//static String fileName = "C:\\Users\\MasSoud\\Dropbox\\acads 1-2\\AOS\\Project2\\logs.txt";
	int csTime = 0;
	int port_no = 0;
	int pid = 0;
	Socket socket= null;
	public CSNode(String[] args) throws IOException, InterruptedException{
		argParsing(args);//gives pid and port_no
	}
	public static void main(String[] args) throws IOException, InterruptedException {
		new CSNode(args).start();
	}
	//server thread
	//Thread serverThread = new Thread(){
	public void run(){
		try {
			ServerSocket serverSocket = new ServerSocket(port_no);
			System.out.println("Starting Server Thread");
			
			while(true){
				socket = serverSocket.accept();
				System.out.println("after accepting .. before readline");
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				System.out.println("after buffer reader");
				String msgToServer = in.readLine();
				System.out.println("after readline");
				System.out.println("-------------------"+msgToServer+"--------------------");
				String msgToProcess[] = msgToServer.split(" ");
				
				csTime = Integer.parseInt(msgToProcess[2]);

				SimpleDateFormat sim = new SimpleDateFormat("hh:mm:ss.SSS");
				String time = sim.format(new Date());

				//Write <SENDER-ID,REQ-ID,START-TIME> to the file.
				String mesgToLog = msgToProcess[0]+" "+msgToProcess[1]+" "+time;
				writeToFile(mesgToLog);

				//Sleeps CST amount of time.
				try {
					Thread.currentThread().sleep(csTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				//Write <SENDER-ID,REQ-ID,FINISH-TIME> to the file.
				String timeafter = sim.format(new Date());
				String mesgToLog1 = msgToProcess[0]+" "+msgToProcess[1]+" "+timeafter;
				writeToFile(mesgToLog1);

				//Sends ACK to the sender notifying that CS request is done.
				PrintWriter pwProcess = new PrintWriter(socket.getOutputStream(), true);
				String sendMesg ="ACK"+" "+"4"+" "+"4";
				pwProcess.println(sendMesg);
//				if(!msgToProcess[0].equals("PING")){
//					
//
//					//ReceiverThread connection = new ReceiverThread(socket, Integer.parseInt(msg[0]));
//					//Thread connThread = new Thread(connection);
//					//connThread.start();
//				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	//};
	//serverThread.start();
	public  void argParsing(String[] args) throws IOException, InterruptedException {
		String firstArg  = args[0];
		String secondArg = args[1];
		pid =Integer.parseInt(firstArg);
		port_no = Integer.parseInt(secondArg);
		System.out.println("Process ID of this system is : "+pid);
	}

	//write to logs.txt
	public void writeToFile(String msg){
		try{
			// Create file 
			System.out.println("inside");
			FileWriter fw = new FileWriter(fileName,true); //the true will append the new data
			fw.write(msg);
			fw.write(System.getProperty("line.separator"));//appends the string to the file
			fw.close();
		}catch (Exception e){//Catch exception if any
			e.printStackTrace();
		}
	}
}

//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@//

//class ReceiverThread implements Runnable{
//	private static Socket socket;
//	static String msgToServer=null;
//	private static int replyFrom = 0;
//	BufferedReader in = null;
//	public ReceiverThread(Socket s, int i) {
//		System.out.println("Starting Receiver Thread for process: "+i);
//		this.setSocket(s);
//		this.setReplyFrom(i);
//		System.out.println("after constructor");
//	}
//
//	@Override
//	public void run() {
//		try {
//			in = new BufferedReader(new InputStreamReader(getSocket().getInputStream()));
//			while(true)
//			{
//				try
//				{
//					System.out.println("inside run");
//					msgToServer = in.readLine();
//					if(null != msgToServer){
//						System.out.println("-------------------"+msgToServer+"--------------------");
//						String msgToProcess[] = msgToServer.split(" ");
//
//						SimpleDateFormat sim = new SimpleDateFormat("hh:mm:ss.SSS");
//						String time = sim.format(new Date());
//
//						//Write <SENDER-ID,REQ-ID,START-TIME> to the file.
//						String mesgToLog = msgToProcess[0]+" "+msgToProcess[1]+" "+time;
//						CSNode.writeToFile(mesgToLog);
//
//						//Sleeps CST amount of time.
//						try {
//							Thread.currentThread().sleep(CSNode.csTime);
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
//
//						//Write <SENDER-ID,REQ-ID,FINISH-TIME> to the file.
//						String timeafter = sim.format(new Date());
//						String mesgToLog1 = msgToProcess[0]+" "+msgToProcess[1]+" "+timeafter;
//						CSNode.writeToFile(mesgToLog1);
//
//						//Sends ACK to the sender notifying that CS request is done.
//						PrintWriter pwProcess = new PrintWriter(getSocket().getOutputStream(), true);
//						String sendMesg ="ACK" ;
//						pwProcess.println(sendMesg);

//					}
//				}catch (IOException e)
//				{
//					System.out.println("yup it blew up here"+e);
//				}
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	public static Socket getSocket() {
//		return socket;
//	}
//
//	public void setSocket(Socket socket) {
//		ReceiverThread.socket = socket;
//	}
//
//	public static int getReplyFrom() {
//		return replyFrom;
//	}
//
//	public void setReplyFrom(int replyFrom) {
//		ReceiverThread.replyFrom = replyFrom;
//	}
//}
