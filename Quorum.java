import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.PriorityBlockingQueue;

public class Quorum extends Thread{
	static int noOfNodes = 0;
	static int noOfMsgs = 0;
	static int ia = 0;
	static int cst = 0;
	static int pid = 0;
	static int port_no = 0;
	static int local_clock = 0;
	static int counter = 0;
	static String[] quorum;
	static String connect = "Connection";
	static int pingCounter = 0;

	static int requestSentCtr = 0;
	static int replyRecCtr = 0;
	static int releaseSentCtr = 0;
	static int failRecCtr = 0;
	static int inquireRecCtr = 0;
	static int yieldSentCtr = 0;


	static int initialCapacity = 11;
	static  Comparator<Priority> comparator = new Priority();
	static PriorityBlockingQueue<Priority> p= new PriorityBlockingQueue<Priority>(initialCapacity, comparator);
	static PriorityBlockingQueue<Priority> inquireQueue= new PriorityBlockingQueue<Priority>(initialCapacity, comparator);
	static boolean isGranted = false;
	static Priority nodeGrnt = null;
	static HashMap<Integer, Boolean> checkReplies = new HashMap<Integer, Boolean>();//include your own resource also
	static boolean checkIfExeCS = false;
	static int reqId = 1;

	//static String input_path = "C:\\Users\\MasSoud\\Dropbox\\acads 1-2\\AOS\\Project2\\input.txt";
	static String input_path = "//people//cs//t//txc121730//input.txt";
	//static String config_path = "C:\\Users\\MasSoud\\Dropbox\\acads 1-2\\AOS\\Project2\\config.txt";
	static String config_path = "//people//cs//t//txc121730//config.txt";

	//static Socket processSocket;
	//static ServerSocket serverSocket;
	//static PrintWriter pwProcess;


	public static HashMap<String, PrintWriter> printWriterMap = new HashMap<String, PrintWriter>();
	public static boolean isFailed = false;
	public static boolean isInquire = false;

	public static void main(String[] args) throws IOException, InterruptedException  {
		argParsing(args);//gives pid and port_no
		readInput();//reads input populates N,M,IA,CST and quorum
		if(pid == 0){
			System.out.println("before initiate");
			initiate();
			System.out.println("after initiate");
			pingCounter = 1;
		}

		initializeReplyMap();//initializes reply map to false

		//Server Thread
		Thread serverThread = new Thread(){
			public void run(){
				try {
					ServerSocket serverSocket = new ServerSocket(port_no);
					System.out.println("Starting Server Thread");
					Socket socket= null;
					while(true){
						socket = serverSocket.accept();
						System.out.println("Creating receiver thread from server socket thread");
						BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						String msgToServer = in.readLine();
						String msg[] = msgToServer.split(" ");

						System.out.println("Putting socket of "+ msg[2] +" in the socketmap");
						PrintWriter pwProcess = new PrintWriter(socket.getOutputStream(), true);
						updateSocketMap(msg[2], pwProcess);

						ReceiveThread connection = new ReceiveThread(socket, Integer.parseInt(msg[2]));
						Thread connThread = new Thread(connection);
						connThread.start();
						pingCounter++;
					}
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		serverThread.start();

		if(pingCounter ==1){
			//currentThread().sleep(1000);
			sendQuorum();
		}
		else{
			System.out.println("waiting till i receive initiate msg from 0");
			while(pingCounter!=1){
				currentThread().sleep(5);
			}
			currentThread().sleep((pid+2)*1000);
			sendQuorum();
		}
	}

	public static void terminate() throws IOException {
		String statistics = requestSentCtr+","+replyRecCtr+","+releaseSentCtr+","+failRecCtr+","+inquireRecCtr+","+yieldSentCtr;
		String sendMesg ="TERMINATE"+" "+local_clock+" "+pid+" "+statistics;
		int i =0;
		sendMsg(i, sendMesg);
	}

	public static void initializeReplyMap() {
		//Initialize the hashmap
		for(int i=0;i<quorum.length;i++){
			//if(!quorum[i].equals(Integer.toString(pid))){
			int j = Integer.parseInt(quorum[i]);
			checkReplies.put(j, false);
			//}
		}
	}

	public static void updateSocketMap(String process, PrintWriter pwriter) {
		//Socket newSocket = socketMap.get(process);
		//if(newSocket != null){
		printWriterMap.put(process, pwriter);
		//}
	}

	public static void argParsing(String[] args) throws IOException, InterruptedException {
		String firstArg  = args[0];
		String secondArg = args[1];
		pid =Integer.parseInt(firstArg);
		port_no = Integer.parseInt(secondArg);
		System.out.println("Process ID of this system is : "+pid);
	}

	public static void initiate() throws IOException {
		//System.out.println("Inside initiate");
		String sendMsg ="PING"+" "+local_clock+" "+"0";
		for(int i=1;i<noOfNodes;i++){
			sendMsg(i, sendMsg);
		}
	}

	public static void sendQuorum() throws NumberFormatException, IOException, InterruptedException{
		System.out.println("inside send quorum");
		for(int j=0;j<noOfMsgs;j++){
			local_clock++;
			int dup_clock = local_clock;
			//			for(int i=0;i<quorum.length;i++){
			//				String sendMsg = "REQUEST"+" "+dup_clock+" "+pid;
			//				sendMsg(Integer.parseInt(quorum[i]), sendMsg);
			//				requestSentCtr++;
			//			}
			for(int i=0;i<quorum.length;i++){
				String sendMsg = "REQUEST"+" "+dup_clock+" "+pid;
				String msgToSelf[] = sendMsg.split(" "); 
				if(!quorum[i].equals(Integer.toString(pid))){
					sendMsg(Integer.parseInt(quorum[i]), sendMsg);
				}
				else{
					System.out.println("inside if message to self case");
					
					Priority selfNode = new Priority(msgToSelf);
					if(p.peek()!=null){
						Priority x = p.peek();
						int comp = x.compare(selfNode, x);
						if(comp == 1){
							System.out.println("my req granted to me");
							local_clock++;
							replyRecCtr++;
							int replyFrom = pid;
							checkReplies.put(replyFrom, true);
							checktoenterCS();
							isGranted = true;
							nodeGrnt = selfNode;
						}
						else{
							p.put(selfNode);
							System.out.println("printing high priority node: "+x.id);
							Quorum.nodeGrnt = x;
							//send reply
							String sendMesg ="REPLY"+" "+local_clock+" "+pid;
							Quorum.local_clock++;
							sendMsg(x.id, sendMesg);
						}
					}
					else{
						System.out.println("my req granted to me");
						local_clock++;
						replyRecCtr++;
						int replyFrom = pid;
						checkReplies.put(replyFrom, true);
						checktoenterCS();
						isGranted = true;
						nodeGrnt = selfNode;
					}
				}
			}
			while(!checkIfExeCS){
				currentThread().sleep(2000);
			}
			currentThread().sleep(ia);
		}
	}

	public static void sendMsg(int i, String sendMesg) throws IOException {
		String smachineName = readConfig(Integer.toString(i));
		//System.out.println("Inside SendMsg");
		InetAddress processIPAddr = InetAddress.getByName(smachineName);  // Fetching the IP address from the hostname
		PrintWriter newPW = printWriterMap.get(Integer.toString(i));

		if(newPW != null){
			System.out.println("connection to "+i+" is already there--> so send: " + sendMesg);
			//System.out.println("Sending To:"+i+": "+sendMesg);
			//PrintWriter pwClient = new PrintWriter(newSocket.getOutputStream(),true);
			newPW.println(sendMesg);
			//local_clock++;
			//pwClient.close();
		}

		else{
			final Socket processSocket = new Socket(processIPAddr, port_no);
			PrintWriter pwProcess = new PrintWriter(processSocket.getOutputStream(), true);
			System.out.println("connection to "+i+" is not there--> so create and send: " + sendMesg);
			//System.out.println("To:"+i+": "+sendMesg);
			printWriterMap.put(Integer.toString(i), pwProcess);
			pwProcess.println(sendMesg);
			//pwProcess.close();
			ReceiveThread connection = new ReceiveThread(processSocket, i);
			Thread t = new Thread(connection);
			t.start();
		}
	}

	//read config file method
	public static String readConfig(String dest) throws FileNotFoundException {
		File configfile = new File(config_path);
		Scanner configip = new Scanner(configfile);
		String machineName="";
		while(configip.hasNext()) {
			String nxtLine = configip.nextLine();
			//System.out.println("nextline is ---" + nxtLine);
			String[] tokens = nxtLine.split(" ");
			if(tokens[0].equals(dest)){
				machineName = tokens[1];
				//System.out.println("machine name is " + machineName);
			}
		}
		configip.close();
		return machineName;
	}

	public static void readInput() throws FileNotFoundException{
		File file = new File(input_path);
		Scanner input;
		input = new Scanner(file);
		while(input.hasNext()) {
			String nextLine = input.nextLine();
			String[] tokens = nextLine.split("=");
			String secondChar = tokens[0].substring(1);

			if(tokens[0].equals("N")){
				noOfNodes = Integer.parseInt(tokens[1]);
				System.out.println("value of N is : "+noOfNodes);
			}
			if(tokens[0].equals("M")){
				noOfMsgs = Integer.parseInt(tokens[1]);
				System.out.println("value of M is : "+noOfMsgs);
			}
			if(tokens[0].equals("IA")){
				ia = Integer.parseInt(tokens[1]);
				System.out.println("value of ia is : "+ia);
			}
			if(tokens[0].equals("CST")){
				cst = Integer.parseInt(tokens[1]);
				System.out.println("value of cst is : "+ cst);
			}

			if(secondChar.equals(Integer.toString(pid))){
				System.out.print("The members of : "+secondChar);	
				quorum = tokens[1].split(",");
				System.out.println(" are "+quorum[0]+" "+quorum[1]+" "+quorum[2]);
			}
		}
		input.close();
	}

	public static synchronized void lamportClock(int sentTime) {
		if(sentTime>=local_clock){
			local_clock  = sentTime + 1;
		}
		else{
			local_clock = local_clock + 1;
		}
		//System.out.println("server time after receive :"+local_clock);
		local_clock++;
	}

	public static void checktoenterCS() throws IOException {
		int counter =0;
		for (int i : checkReplies.keySet()) {
			if(checkReplies.get(i)){
				counter++;				
			}
		}
		if(counter==(quorum.length)){
			//send msg to cs node
			String sendMesg = pid+" "+reqId+" "+cst;
			System.out.println("send message to CSNode "+sendMesg);
			Quorum.local_clock++;
			sendMsg(noOfNodes, sendMesg);
			reqId++;
			checkIfExeCS = true;
			//initializeReplyMap();
		}
		else{
			//wait for other nodes
			System.out.println("Waiting for replies from other nodes to enter critical section");
			//			while(true){
			//				if(counter==(quorum.length))
			//				break;
			//			}
		}
		//return checkIfExeCS;
	}

	public static void initializeStats() {
		requestSentCtr = 0;
		replyRecCtr = 0;
		releaseSentCtr = 0;
		failRecCtr = 0;
		inquireRecCtr = 0;
		yieldSentCtr = 0;
	}
}


//################################################################################################################################//


class ReceiveThread implements Runnable{
	static Socket socket;
	static String msgToServer=null;
	static int replyFrom = 0;

	public ReceiveThread(Socket s, int i) {
		System.out.println("Starting Receiver Thread for process: "+i);
		this.socket=s;
		this.replyFrom = i;
		System.out.println("after constructor");
	}

	@Override
	public void run() {
		try {
			//System.out.println("inside run");
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			//System.out.println("after buffer reader");
			while(true)
			{
				//System.out.println("inside while");
				try
				{
					msgToServer = in.readLine();
					if(null != msgToServer){
						System.out.println("-------------------"+msgToServer+"--------------------");
						String msgToProcess[] = msgToServer.split(" ");
						if(!msgToProcess[0].equals("ACK")){
							int sentTime = Integer.parseInt(msgToProcess[1]);
							Quorum.lamportClock(sentTime);	
						}

						if(msgToProcess[0].equals("PING")){
							System.out.println("Got initiate message from process: 0 ");
							//Quorum.pingCounter++;
						}
						else if(msgToProcess[0].equals("REQUEST")){
							if(Quorum.nodeGrnt !=null){//token is granted to other process 
								System.out.println("inside if REQUEST is already granted");
								Priority node = new Priority(msgToProcess);
								Quorum.p.put(node);//put it in priority queue
								System.out.println("putting REQUEST in priority queue");
								//compare node and granted node .. based on comparison Inqire or failed
								int checkPriority = node.compare(Quorum.nodeGrnt, node);
								if(checkPriority == -1){
									//send Inquire nodeGrnt
									System.out.println("inside if new REQUEST priority is high ");
									Quorum.local_clock++;
									if(Quorum.nodeGrnt.id != Quorum.pid){
										String sendMesg = "INQUIRE"+" "+Quorum.local_clock+" "+Quorum.pid;
										Quorum.sendMsg(Quorum.nodeGrnt.id, sendMesg);
									}
									else{
										String sendMesg= "REPLY"+" "+Quorum.local_clock+" "+Quorum.pid;
										Quorum.sendMsg(Integer.parseInt(msgToProcess[2]), sendMesg);
										//Quorum.nodeGrnt = null;
									}
								}else{
									//send failed to node
									System.out.println("inside if new REQUEST priority is low ");
									String sendMesg= "FAILED"+" "+Quorum.local_clock+" "+Quorum.pid;
									Quorum.local_clock++;
									Quorum.sendMsg(Integer.parseInt(msgToProcess[2]), sendMesg);
								}
							}
							else{
								//it is not granted to any process
								System.out.println("inside if REQUEST is not granted");
								Priority node = new Priority(msgToProcess);
								Quorum.p.put(node);//put it in priority queue
								//Quorum.nodeGrnt = new Priority(msgToProcess);
								
								try {
									Thread.currentThread().sleep(5000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								
								Priority newNode = Quorum.p.poll();
								Quorum.nodeGrnt = newNode;
								//Quorum.isGranted = true;
								//send reply
								String sendMesg ="REPLY"+" "+Quorum.local_clock+" "+Quorum.pid;
								Quorum.local_clock++;
								if(newNode.id == Quorum.pid){
									System.out.println("my req granted to me");
									Quorum.replyRecCtr++;
									int replyFrom = Quorum.pid;
									Quorum.checkReplies.put(replyFrom, true);
									Quorum.checktoenterCS();
								}
								else{
									Quorum.sendMsg(newNode.id, sendMesg);
								}
							}
						}
						else if(msgToProcess[0].equals("REPLY")){
							//update map and 
							System.out.println("inside if REPLY");
							Quorum.replyRecCtr++;
							int replyFrom = Integer.parseInt(msgToProcess[2]);
							Quorum.checkReplies.put(replyFrom, true);
							Quorum.checktoenterCS();
						}
						else if(msgToProcess[0].equals("FAILED")){
							Quorum.failRecCtr++;
							Quorum.isFailed  = true;
							System.out.println("inside if REQUEST->FAILED: "+" wait for resource");
						}
						else if(msgToProcess[0].equals("INQUIRE")){
							//check for replies from other nodes and decide
							// if all replies received send failed
							Quorum.inquireRecCtr++;
							System.out.println("inside if INQUIRE");
							Priority node = new Priority(msgToProcess);
							Quorum.inquireQueue.put(node);//put it in inquiry priority queue
							Priority newNode = Quorum.inquireQueue.poll();
							
							Quorum.isInquire = true;
							if(!Quorum.isFailed){
								//send failed
								System.out.println("inside if all replies received");
								Quorum.inquireQueue.clear();
							}
							// else send yield
							else{
								//send yield
								System.out.println("inside if all replies are not received");
								String sendMesg ="YIELD"+" "+Quorum.local_clock+" "+Quorum.pid;
								Quorum.local_clock++;
								Quorum.sendMsg(newNode.id, sendMesg);
								Quorum.yieldSentCtr++;
							}
						}
						else if(msgToProcess[0].equals("YIELD")){
							// send reply to node and remove from queue .. Use Poll()
							System.out.println("Inside if YIELD");
							Priority newNode = Quorum.p.poll();
							String sendMesg ="REPLY"+" "+Quorum.local_clock+" "+Quorum.pid;
							Quorum.local_clock++;
							Quorum.sendMsg(newNode.id, sendMesg);
							// put the yielded node in the queue
							Priority node = new Priority(msgToProcess);
							Quorum.p.put(node);
						}
						else if(msgToProcess[0].equals("ACK")){
							//send release to all quorum members including itself
							System.out.println("inside if ACK");
							for(int i=0;i<Quorum.quorum.length;i++){
								String sendMsg = "RELEASE"+" "+Quorum.local_clock+" "+Quorum.pid;
								Quorum.local_clock++;
								if(Integer.parseInt(Quorum.quorum[i])!= Quorum.pid){
									Quorum.sendMsg(Integer.parseInt(Quorum.quorum[i]), sendMsg);
								}
								else{
									Quorum.nodeGrnt = null;
								}
								Quorum.releaseSentCtr++;
								Quorum.checkIfExeCS = false;
							}
						}
						else if(msgToProcess[0].equals("RELEASE")){
							//if queue is not empty then poll the queue and send reply
							if(Quorum.p.peek() != null){
								Priority newNode = Quorum.p.poll();
								String sendMesg ="REPLY"+" "+Quorum.local_clock+" "+Quorum.pid;
								Quorum.local_clock++;
								Quorum.sendMsg(newNode.id, sendMesg);
								Quorum.nodeGrnt = newNode;
							}
							//if queue is empty then update isGranted Flag to false
							else{
								Quorum.isGranted = false;
								//intialize hashmap replies to false
								Quorum.initializeReplyMap();
								Quorum.terminate();
								Quorum.initializeStats();
								Quorum.isFailed = false;
								Quorum.isInquire = false;
								Quorum.inquireQueue.clear();
							}
						}
						else if(msgToProcess[0].equals("TERMINATE")){
							System.out.println("inside if TERMINATE");
							String stats = msgToProcess[3];
							System.out.println("requestSentCtr,replyRecCtr,releaseSentCtr,failRecCtr,inquireRecCtr,yieldSentCtr are: "+stats+" respectively");
						}
					}
				}catch (IOException e)
				{
					System.out.println("yup it blew up here"+e);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
class Priority implements Comparator<Priority>{
	int id=0;
	int ts=0;
	int reqid=0;

	public Priority() {

	}
	public Priority(String[] msg) {
		this.ts = Integer.parseInt(msg[1]);
		this.id = Integer.parseInt(msg[2]);
	}

	@Override
	public int compare(Priority o1, Priority o2) {
		//if(QuorumLogic.msgReceived[1].)
		if(o1.ts<o2.ts){
			return 1;
		}else if(o1.ts>o2.ts){
			return -1;
		}else{
			if(o1.id<o2.id){
				return 1;
			}else if(o1.id>o2.id){
				return -1;
			}
		}
		return 0;
	}

}
