import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class Ping {

    short port;
    String IP;
    int numFiles;
    int sizeOfFiles;

    public Ping(short port, String IP, int numFiles, int sizeOfFiles) {
        this.port = port;
        this.IP = IP;
        this.numFiles = numFiles;
        this.sizeOfFiles = sizeOfFiles;
    }

    public Ping(byte[] bytes) throws UnknownHostException {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        this.port = buffer.getShort();
        byte[] ip = new byte[4];
        buffer.get(ip);
        this.IP = InetAddress.getByAddress(ip).getHostAddress();
        this.numFiles = buffer.getInt();
        this.sizeOfFiles = buffer.getInt();
    }

    public byte[] toBytes() throws UnknownHostException {
        byte[] bytes = new byte[14];

        ByteBuffer port = ByteBuffer.allocate(2);
        port.putShort(this.port);
        System.arraycopy(port.array(), 0, bytes, 0, port.capacity());

        byte[] ip = InetAddress.getByName(this.IP).getAddress();
        System.arraycopy(ip, 0, bytes, 2, ip.length);

        ByteBuffer num = ByteBuffer.allocate(8);
        num.putInt(this.numFiles);
        num.putInt(this.sizeOfFiles);
        System.arraycopy(num.array(), 0, bytes, port.capacity() + ip.length, num.capacity());

        return bytes;
    }

    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        if (obj.getClass() != this.getClass())
            return false;

        Ping p = (Ping) obj;

        return p.IP.equals(this.IP) && p.port == this.port;
    }

    public String PrintPeer() {
        System.out.println( "Port: " + this.port + " IP: " + this.IP);
        return null;
    }
}