import java.lang.*;
import java.util.*;

// javac -g HelloWorld.java
public class HelloWorld {
	static public void main(String[] args) throws Throwable {
		byte[] data = new byte[128 * 1024 * 1024]; // 128 MB
		data[0] = 1;
		data[10000] = 2;
		data[data.length - 1] = 3;
		//byte[] data = new byte[64 * 1024 * 1024]; // 64 MB
		System.out.println("HELLO");
		//Thread.sleep(1000);
	}
}
