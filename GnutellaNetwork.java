import java.util.ArrayList;
import java.util.List;
import java.io.*;
import java.net.*;
import java.nio.file.Files;

public class GnutellaNetwork {

	public final static short def_port = 1234;
    public static String def_directory = "C:\\Users\\smada\\Desktop\\PA4files";

    public static void main(String[] args) throws Exception {	
        GnutellaNetwork GN = new GnutellaNetwork();

        for (int i=0;i<args.length;i++) {
            try {
                switch (args[i++]) {
                case "port":
                    GN.setPort(Integer.parseInt(args[i]));
                    break;
                case "connect":
                    GN.connect(args[i]);
                    break;
                case "setdirectory":
                	GN.setdirectory(args[i]);
                	break;
                case "query":
                	GN.queryreq(args[i], Integer.parseInt(args[++i]));
                	break;
                default:
                    System.err.println("Invalid argument: " + args[i - 1]);
                    return;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Invalid arguments");
            }
        }

        GN.start();
    }

    ArrayList<Peer> peers = new ArrayList<>();
    ArrayList<File> files = new ArrayList<>();
    ArrayList<Query> queries = new ArrayList<>();
    
    Ping newping;
    String directory = def_directory;

    GnutellaNetwork() {
        this.newping = new Ping(def_port, getLocalIP(), 0, 0);
    }

    public void setPort(int port) {
        newping.port = (short) port;
        newping.PrintPeer();
    }
    
    public void queryreq(String searchstring, int ttl){
    	queries.add(new Query(newping.IP, (short) (newping.port + queries.size() + 1), searchstring, ttl));
    }

    public void connect(String addr) {
        String[] inp = addr.split(":", 2);
        String ip = inp[0];
		
		short connectPort = Short.parseShort(inp[1]);

        peers.add(new Peer(new Ping(connectPort, ip, 0, 0), System.currentTimeMillis()));
    }
    
    public void setdirectory(String directory){
    	this.directory = directory;
    }


    public void start() {
        ListenPing LP = new ListenPing();
        SendPing SP = new SendPing();
        FileDirectory FD = new FileDirectory(directory);
		
        ArrayList<FileDownloader> filedl = new ArrayList<>();
        for (Query query : queries) {
        	filedl.add(new FileDownloader(query));
        }

        LP.start();
        SP.start();
		FD.start();

        for (FileDownloader fd : filedl) {
            fd.start();
        }
    }

    private class SendPing extends Thread {

        public void run() {
            while (true) {
                try (DatagramSocket csock = new DatagramSocket()) {
                    ArrayList<Peer> deadpeers = new ArrayList<>();

                    for (Peer peer : peers) {
                        if (System.currentTimeMillis() - peer.lastMessage > 100000)
                            deadpeers.add(peer);
                        else {
                            byte[] Datatosend = newping.toBytes();
                            DatagramPacket Packettosend = new DatagramPacket(Datatosend, Datatosend.length,InetAddress.getByName(peer.ping.IP), peer.ping.port);
                            csock.send(Packettosend);
                        }
                    }
                    if (deadpeers.size() > 0){
                        peers.removeAll(deadpeers);
                        System.out.println("Peer is dead!");
                    }

                    Thread.sleep(60000);
                } catch (IOException e) {
                	// TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        }
    }

    private class ListenPing extends Thread {

        public void run() {
            try {
            	DatagramSocket servsock = new DatagramSocket(newping.port);
                while (true) {
                    byte[] Datarecv = new byte[1024];
                    DatagramPacket Packetrecv = new DatagramPacket(Datarecv, Datarecv.length);
                    servsock.receive(Packetrecv);
                    byte[] data = Packetrecv.getData();
                    if (Packetrecv.getLength() == 14) {
                        Ping Pingrecv = new Ping(data);
                        DataProcess dp = new DataProcess(Pingrecv);
                        dp.start();
                    } else {
                        Query Queryrecv = new Query(data);
                        QueryProcess qp = new QueryProcess(Queryrecv);
                        qp.start();
                    }
                }
            } catch (IOException e) {
            	// TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private class DataProcess extends Thread {

        Ping ping;

        public DataProcess(Ping ping) {
            this.ping = ping;
        }

        public void run() {
            if (ping.equals(newping))
                return;

            boolean found = false;
            for (Peer peer : peers) {
                if (peer.ping.equals(ping)) {
                    peer.lastMessage = System.currentTimeMillis();
                    peer.ping = ping;
                    found = true;
                }
            }

            if (!found){
                peers.add(new Peer(ping, System.currentTimeMillis()));
                System.out.println("New Peer added!");
                System.out.println("Number of peers: " + peers.size());
            }

            try {
            	DatagramSocket csock = new DatagramSocket();
                byte[] Datatosend = newping.toBytes();

                DatagramPacket Packettosend = new DatagramPacket(Datatosend, Datatosend.length,InetAddress.getByName(ping.IP), ping.port);
                csock.send(Packettosend);

                Datatosend = ping.toBytes();
                for (Peer peer : peers) {
                    if (peer.ping.equals(ping))
                        continue;
                    Packettosend = new DatagramPacket(Datatosend, Datatosend.length, InetAddress.getByName(peer.ping.IP),peer.ping.port);
                    csock.send(Packettosend);
                }
                csock.close();
            } catch (IOException e) {
            	// TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private class FileDownloader extends Thread {

        Query query;
        
        public FileDownloader(Query query) {
            this.query = query;
        }

        public void run() {
        	System.out.println("Search string: " + query.searchstring);
            try {
            	DatagramSocket csock = new DatagramSocket();
                for (Peer peer : peers) {
                    byte[] Datatosend = this.query.toBytes();
                    DatagramPacket Packettosend = new DatagramPacket(Datatosend, Datatosend.length,InetAddress.getByName(peer.ping.IP), peer.ping.port);
                    csock.send(Packettosend);
                }
                csock.close();
            } catch (IOException e) {
            	// TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
            	ServerSocket servsocket = new ServerSocket(query.reqport);
                Socket socket = servsocket.accept();
                DataInputStream in = new DataInputStream(socket.getInputStream());
                int dataLength = in.readInt();
                byte[] data = new byte[dataLength];
                in.read(data);
                FileOutputStream fos = new FileOutputStream(directory + query.searchstring);
                fos.write(data);

                fos.close();
                in.close();
                servsocket.close();
                
                System.out.println("File downloaded! " + data.length + " bytes");
            } catch (IOException e) {
            	// TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private class QueryProcess extends Thread {

        Query query;

        public QueryProcess(Query query) {
            this.query = query;
        }

        public void run() {
            if (System.currentTimeMillis() - query.timestamp >= query.ttl)
                return;

            for (File file : files) {
                if (query.searchstring.equals(file.getName())) {
                    try {
                    	Socket socket = new Socket(query.reqIP, query.reqport);
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                        byte[] filedata = Files.readAllBytes(file.toPath());
                        out.writeInt(filedata.length);
                        out.write(filedata);
                        out.close();
                        socket.close();
                        System.out.println("File found and sent. " + filedata.length + " bytes sent");
                    } catch (IOException e) {
                    	// TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    return;
                }
            }
            
            try {
            	DatagramSocket csock = new DatagramSocket();
            	
                byte[] Datatosend = query.toBytes();
                
                //System.out.println("File not found!");

                for (Peer peer : peers) {
                    DatagramPacket Packettosend = new DatagramPacket(Datatosend, Datatosend.length,InetAddress.getByName(peer.ping.IP), peer.ping.port);
                    csock.send(Packettosend);
                }
                csock.close();
            } catch (IOException e) {
            	// TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private class FileDirectory extends Thread {

        private String folder;

        public FileDirectory(String folder) {
            this.folder = folder;
        }

        public void run() {
            File fls = new File(folder);
            fls.mkdirs();

            while (true) {
                peerupdate();
                try {
                    Thread.sleep(30000);;
                } catch (InterruptedException e) {
                	// TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        public void peerupdate(){
            filefinder(folder, files);
            int filesize = 0;
            for (File file : files) {
            	filesize=(int) (filesize+file.length());
            }

            newping.sizeOfFiles = filesize;
            newping.numFiles = files.size();
            System.out.println("Number of files: " + files.size());
        }

        private void filefinder(String folder, List<File> f) {
            File directory = new File(folder);

            File[] filist = directory.listFiles();
            if (filist!=null) {
                for (File file : filist) {
                    if (file.isFile() && !files.contains(file)) {
                        f.add(file);
                    } else if (file.isDirectory()) {
                    	filefinder(file.getAbsolutePath(), f);
                    }
                }
            }
        }
    }

    public static String getLocalIP() {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 8080);
            return socket.getLocalAddress().getHostAddress();
        } catch (IOException e) {
        	// TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }
}