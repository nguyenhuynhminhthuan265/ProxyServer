import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Map;

public class ProxyServer {
	String forbiddenResponse = "<!DOCTYPE html>\r\n" + "<html lang=\"en\">\r\n" + "<head>\r\n"
			+ "    <meta charset=\"UTF-8\">\r\n"
			+ "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\r\n"
			+ "    <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">\r\n" + "    <title>Document</title>\r\n"
			+ "</head>\r\n" + "<body style = \"background: aquamarine\">\r\n" + "    <section>\r\n"
			+ "        <h1 style = \"font-size: 32px; text-align: center\">403</h1>\r\n" + "        <hr>\r\n"
			+ "        <h2 style = \"text-align: center\">FORBIDDEN PAGE</h2>\r\n" + "    </section>\r\n"
			+ "</body>\r\n" + "</html>";

	public static int port = 8888;
	final static String KY_TU_XUONG_DONG = "\r\n";
	public static ServerSocket serversocket;

	public static Map<String, String> cache = new Hashtable<String, String>();

	public synchronized static void cache(ReadHttpRequest request, ReadHttpResponse response) throws IOException {
		File file;
		DataOutputStream dataOut;

		file = new File("cache/", "cached_" + System.currentTimeMillis());
		dataOut = new DataOutputStream(new FileOutputStream(file));
		dataOut.writeBytes(response.toString());
		dataOut.write(response.body);
		dataOut.close();
		cache.put(request.URL, file.getAbsolutePath());

	}

	@SuppressWarnings("resource")
	public synchronized static byte[] uncache(String url) throws IOException {
		File file;
		FileInputStream fileInput;
		String hashfile;
		byte[] bytescache;

		if ((hashfile = cache.get(url)) != null) {
			file = new File(hashfile);
			fileInput = new FileInputStream(file);
			bytescache = new byte[(int) file.length()];
			fileInput.read(bytescache);

			return bytescache;
		} else {

			return bytescache = new byte[0];
		}

	}

	public static void main(String[] args) {
		int myPort = port;
		ProxyServer outer = new ProxyServer();
		File cached = new File("cache/");
		if (!cached.exists()) {
			cached.mkdir(); // táº¡o Ä‘Æ°á»�ng dáº«n thÆ° má»¥c
		}

		myPort = port;
		try {
			serversocket = new ServerSocket(port);
		} catch (IOException e) {
			System.out.println("khong tao duoc socket !!!");
			System.exit(-1);
		}

		Socket client = null;

		while (true) {
			try {

				client = serversocket.accept();
				(new Thread(outer.new Threads(client))).start();
			} catch (IOException e) {
				System.out.println("khong the doc request tu client!!!");

				continue;
			}
		}

	}

	class ReadHttpRequest {

		final static int HTTP_PORT = 80;

		String method;
		String URL;
		String version1;
		String header1 = "";

		public String host;
		public int port;

		public String getHost() {
			return host;
		}

		public int getPort() {
			return port;
		}

		public ReadHttpRequest(BufferedReader buffRead) {
			String sLine = "";
			try {
				sLine = buffRead.readLine();
			} catch (IOException e) {
				System.out.println("khong doc duoc dong request!!!");
			}
			String[] temp = sLine.split(" ");// cáº¯t chuá»—i request thÃ nh máº£ng chuá»—i
			// format GET=GET http://www.google.pt HTTP/1.0
			method = temp[0];// Method GET
			URL = temp[1]; // URI
			version1 = temp[2];// HTTP version
			System.out.println("URI: " + URL);
			if (!method.equals("GET")) {
				System.out.println("Method khong phai GET");

			}
			try {
				String line = buffRead.readLine();
				while (line.length() != 0) {
					header1 += line + KY_TU_XUONG_DONG;
					if (line.startsWith("Host: ")) {
						temp = line.split(" ");
						if (temp[1].indexOf(':') > 0) {
							String[] temp2 = temp[1].split(":");
							host = temp2[0];
							port = Integer.parseInt(temp2[1]);
						} else {
							host = temp[1];
							port = HTTP_PORT;
						}
					}

					line = buffRead.readLine();

				}
			} catch (IOException e) {
				System.out.println("Khong doc duoc tu socket!!!");
				return;
			}
			// System.out.println("Host káº¿t ná»‘i: " + host + " táº¡i port: " + port);
			System.out.println(toString());
		}

		// In ra retuest
		@Override
		public String toString() {
			String req = "";

			req = method + " " + URL + " " + version1 + KY_TU_XUONG_DONG;
			req += header1;
			req += "Connection: close" + KY_TU_XUONG_DONG;
			req += KY_TU_XUONG_DONG;

			return req;
		}

	}

	class ReadHttpResponse {
		final static int SIZE = 8192;

		String version2;
		int status;
		String inforLine = "";
		String header2 = "";
		public byte[] body = new byte[100000];

		public ReadHttpResponse(DataInputStream fromServer) {
			int iLength = -1;
			boolean bInforLine = false;
			try {
				String line = fromServer.readLine();
				while (line.length() != 0) {
					if (!bInforLine) {
						inforLine = line;
						bInforLine = true;
					} else {
						header2 += line + KY_TU_XUONG_DONG;
					}

					if (line.startsWith("Content-Length:") || line.startsWith("Content-length:")) {
						String[] tmp = line.split(" ");
						iLength = Integer.parseInt(tmp[1]);
					}
					line = fromServer.readLine();
				}
			} catch (IOException e) {
			}

			try {
				int bytesRead = 0;
				byte buf[] = new byte[SIZE];
				boolean loop = false;

				if (iLength == -1) {
					loop = true;
				}

				while (bytesRead < iLength || loop) {

					int res = fromServer.read(buf, 0, SIZE);
					if (res == -1) {
						break;
					}

					for (int i = 0; i < res && (i + bytesRead) < 100000; i++) {
						body[bytesRead + i] = buf[i];
					}
					bytesRead += res;
				}
				System.out.println(toString());
			} catch (IOException e) {
				System.out.println(" Khong doc dc response body: " + e);
				return;
			}
		}

		@Override
		public String toString() {
			String res = "";

			res = inforLine + KY_TU_XUONG_DONG;
			res += header2;
			res += KY_TU_XUONG_DONG;

			return res;
		}

	}

	class Threads implements Runnable {
		public final Socket client;

		public Threads(Socket client) {
			this.client = client;
		}

		@Override
		public void run() {
			Socket server = null;
			ReadHttpRequest req = null;
			ReadHttpResponse res = null;

			// Nháº­n request tá»« browser client
			try {
				BufferedReader fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
				req = new ReadHttpRequest(fromClient);

				File fis = new File("blacklist.conf");
				FileReader dis = new FileReader(fis);
				BufferedReader blackListHost = new BufferedReader(dis);
				String Line;
				boolean bFlag = false;
				while ((Line = blackListHost.readLine()) != null) {
					if (Line.equals(req.host)) {

						System.out.println("Host bi cam !!!" + req.host);
						bFlag = true;
						break;

					}

				}
				if (bFlag == true) {
					FileOutputStream fos = new FileOutputStream("forbidden.html");
					DataOutputStream dos = new DataOutputStream(fos);
					dos.writeChars(forbiddenResponse);
					File htmlFile = new File("forbidden.html");
					// Desktop.getDesktop().browse(htmlFile.toURI());

					DataOutputStream toClient = new DataOutputStream(client.getOutputStream());
					toClient.writeBytes(forbiddenResponse);

					client.close();
					server.close();
					wait();
				}

			} catch (IOException e) {
				// System.out.println("khÃ´ng Ä‘á»�c Ä‘Æ°á»£c request tá»« client !!!");
				return;
			} catch (InterruptedException e) {
				throw new RuntimeException("Thread interrupted...\n" + e);

			}

			// Chuyá»ƒn request lÃªn Server
			try {

				server = new Socket(req.getHost(), req.getPort());
				DataOutputStream toServer = new DataOutputStream(server.getOutputStream());
				toServer.writeBytes(req.toString());
			} catch (UnknownHostException e) {
				System.out.println("khong xac dinh duoc host: " + req.getHost());
				System.out.println(e);
				return;
			} catch (IOException e) {
				return;
			}

			// Nháº­n response tá»« Server vÃ  chuyá»ƒn lÃªn Client

			try {
				byte[] cache = ProxyServer.uncache(req.URL);
				if (cache.length == 0) { // chÆ°a cache
					DataInputStream fromServer = new DataInputStream(server.getInputStream());
					res = new ReadHttpResponse(fromServer);
					DataOutputStream toClient = new DataOutputStream(client.getOutputStream());

					toClient.writeBytes(res.toString());
					toClient.write(res.body);

					ProxyServer.cache(req, res);

					client.close();
					server.close();

				} else { // Ä‘Ã£ cache
					DataOutputStream toClient = new DataOutputStream(client.getOutputStream());
					toClient.write(cache);
					client.close();
					server.close();
				}

			} catch (IOException e) {
				System.out.println("khong tra duoc response den client: " + e);
			}

		}

	}

}
