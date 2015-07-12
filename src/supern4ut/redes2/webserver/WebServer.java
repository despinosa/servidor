/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package supern4ut.redes2.webserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLConnection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebServer {

    public static final int PUERTO = 8000;
    static final String OK = "HTTP/1.1 200 Okay\n";
    static final String ERROR_ = "HTTP/1.1 500 Internal error\n\n";
    static final String NOTIMPL_ = "HTTP/1.1 501 Not implemented\n\n";
    static final String NOTFOUND_ = "HTTP/1.1 404 Not found\n\n";
    static final Pattern POSTPARAMS = Pattern.compile("(\\w+=\\w+)" +
                                                      "(\\&\\w+=\\w+)*");
    ServerSocket server;

    class Manejador extends Thread {

        protected Socket socket;
        protected PrintWriter writer;
        protected BufferedOutputStream buffOut;
        protected BufferedReader reader;
        protected String header;

        public Manejador(Socket socket) throws Exception {
            this.socket = socket;
            header = "Server: Custom simple HTTP server\n";
        }

        @Override
        public void run() {
            String line, filePath;
            Matcher paramsMatcher;

            header += "Date: " + new Date() + "\n";
            System.out.println("\ncliente conectado en " +
                               socket.getInetAddress());
            System.out.println("puerto " + socket.getPort());
            try {
                reader = new BufferedReader(new InputStreamReader(socket.
                        getInputStream()));
                buffOut = new BufferedOutputStream(socket.getOutputStream());
            } catch (IOException ex) {
                Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE,
                                                                null, ex);
            }
            try {
                line = reader.readLine();
                System.out.println("petición: " + line + "\n\n");
                writer = new PrintWriter(new OutputStreamWriter(buffOut));
                if (line == null) {
                    writer.print(OK + header + "Content-type: text/html\n\n");
                    writer.print("<html>\n" +
                                 "\t<head><title>HOLA</title></head>\n" +
                                 "\t<body bgcolor='#AACCFF'>\n" +
                                 "<br>Linea Vacia</br>" +
                                 "\t</body>\n" +
                                 "</html>");
                } else if(line.toUpperCase().startsWith("HEAD")) {
                    writer.write(OK + header);
                } else if(line.toUpperCase().startsWith("GET")) {
                    filePath = parseRequest(line);
                    filePath = filePath.split("\\?")[0];
                    System.out.println("enviando " + filePath);
                    sendFile(filePath);
                } else if(line.toUpperCase().startsWith("POST")) {
                    do {
                        line = reader.readLine();
                        paramsMatcher = POSTPARAMS.matcher(line);
                    } while(!paramsMatcher.matches());
                    writer.print(OK + header + "Content-type: text/html\n\n");
                    writer.print("<html>\n" +
                                 "\t<head><title>HOLA</title></head>\n" +
                                 "\t<body style='background-color:#FFAACC'>\n" +
                                 "\t\t<h1>Petición POST</h1>\n" +
                                 "\t\t<table>\n" +
                                 "\t\t\t<tr>\n" +
                                 "\t\t\t\t<th>Variable</th><th>Valor</th>\n" +
                                 "\t\t\t</tr>\n");
                    for(String entry : line.split("\\&")) {
                        writer.print("\t\t\t<tr>\n" +
                                     "\t\t\t\t<td>" +
                                     entry.replace("=", "</td><td>") + "</td>\n"
                                     + "\t\t\t</tr>\n");
                    }
                    writer.print("\t\t</table>\n" +
                                 "\t</body>\n" +
                                 "</html>");
                } else {
                    writer.print(NOTIMPL_);
                }
                writer.flush();
                buffOut.flush();
            } catch (IOException ex) {
                writer.print(ERROR_);
                Logger.getLogger(WebServer.class.getName()).
                        log(Level.SEVERE, null, ex);
            } finally {
                try {
                    socket.close();
                } catch (IOException ex) {
                    Logger.getLogger(WebServer.class.getName()).
                            log(Level.SEVERE, null, ex);
                }
            }
        }

        public String parseRequest(String line) {
            int i, f;
            i = line.indexOf("/");
            f = line.indexOf(" ", i);
            if(f == i+1) return "index.html";
            return line.substring(i + 1, f);
        }

        public void sendFile(String fileName) throws IOException {
            int read;
            BufferedInputStream buffIn;
            byte[] buffer;

            try {
                buffIn = new BufferedInputStream(new FileInputStream(fileName));
            } catch (FileNotFoundException ex) {
                System.out.println("archivo no encontrado");
                writer.print(NOTFOUND_);
                writer.flush();
                return;
            }
            writer.print(OK + header + "Content-type: " +
                         URLConnection.guessContentTypeFromStream(buffIn) +
                         "\n\n");
            writer.flush();
            try {
                buffer = new byte[1024];
                buffIn.reset();
                read = buffIn.read(buffer, 0, 1024);
                while(read != -1) {
                    buffOut.write(buffer, 0, read);
                    read = buffIn.read(buffer, 0, 1024);
                }
                buffOut.flush();
                System.out.println("archivo enviado");
            } finally {
                buffIn.close();
            }
        }
    }

    public WebServer() throws Exception {
        Socket client;

        this.server = new ServerSocket(PUERTO);
        System.out.println("servidor iniciado");
        System.out.println("esperando clientes");
        for (;;) {
            client = server.accept();
            new Manejador(client).start();
        }
    }

    public static void main(String[] args) throws Exception {
        WebServer webServ = new WebServer();
    }

}
