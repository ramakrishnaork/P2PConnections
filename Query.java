import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Random;

public class Query {

    
    
    String reqIP;
    short reqport;
    String searchstring;
    int id;
    long ttl;
    long timestamp;

    public Query(String reqIP, short reqport, String searchstring, long ttl) {
        this.reqIP = reqIP;
        this.reqport = reqport;
        this.searchstring = searchstring;
        this.ttl = ttl;
        this.timestamp = System.currentTimeMillis();
        this.id = new Random().nextInt();
        if (this.id < 0)
            this.id = this.id * -1;
    }

    public Query(byte[] bytes) throws UnknownHostException {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        this.reqport = buffer.getShort();
        byte[] ip = new byte[4];
        buffer.get(ip);
        this.reqIP = InetAddress.getByAddress(ip).getHostAddress();
        this.id = buffer.getInt();
        this.ttl = buffer.getLong();
        this.timestamp = buffer.getLong();
        int strlen = buffer.getInt();
        byte[] str = new byte[strlen];
        buffer.get(str);
        this.searchstring = new String(str);
    }

    public byte[] toBytes() throws UnknownHostException {
        byte[] bytes = new byte[30 + this.searchstring.length()];

        ByteBuffer port = ByteBuffer.allocate(2);
        port.putShort(this.reqport);
        System.arraycopy(port.array(), 0, bytes, 0, port.capacity());

        byte[] ip = InetAddress.getByName(this.reqIP).getAddress();
        System.arraycopy(ip, 0, bytes, 2, ip.length);

        ByteBuffer num = ByteBuffer.allocate(4 + 8 + 8 + 4);
        num.putInt(this.id);
        num.putLong(this.ttl);
        num.putLong(this.timestamp);
        num.putInt(this.searchstring.length());
        System.arraycopy(num.array(), 0, bytes, port.capacity() + ip.length, num.capacity());
        System.arraycopy(this.searchstring.getBytes(), 0, bytes, port.capacity() + ip.length + num.capacity(), this.searchstring.length());

        return bytes;
    }
}