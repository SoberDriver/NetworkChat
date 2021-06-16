import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private MyServer myServer;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    private String name;

    public String getName(){
        return name;
    }

    public ClientHandler(MyServer myServer, Socket socket) {
        try {
            this.myServer = myServer;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                try {

                    authentication();
                    readMessages();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }
            }).start();

        }catch (IOException e) {
            throw new RuntimeException("Проблемы при создании обработчика клиента.");
        }
    }

    public void authentication() throws IOException{
        while (true) {
            String str = in.readUTF();
            if (str.startsWith("/auth")){
                String[] parts = str.split("\\s");
                String nick = myServer.getAuthService().getNickByLoginPass(parts[1], parts[2]);
                if (nick != null) {
                        if (!myServer.isNickBusy(nick)){
                            sendMsg("/authok " + nick);
                            name = nick;
                            sendMsg("/help для списка команд");
                            myServer.broadcastMsg(name + " зашел в чат.");
                            myServer.subscribe(this);
                            return;
                        } else {
                            sendMsg("Учетная запись уже используется");
                        }
                } else {
                    sendMsg("Неверные логин или пароль");
                }
            }
        }
    }

    public void readMessages() throws IOException{

        while (true) {
            String str = in.readUTF();
            if (str.startsWith("/")) {
                if (str.equals("/end")) {
                    break;
                }
                if (str.startsWith("/w ")) {
                    String[] tokens = str.split("\\s");
                    String nick = tokens[1];
                    String msg = str.substring(4 + nick.length());
                    myServer.whisperMessage(this, nick, msg);
                }
                if (str.startsWith("/nc")) {
                    String[] parts = str.split("\\s");
                    String newNick = myServer.getAuthService().changeNickname(parts[1], parts[2]);
                    if (newNick != null) {
                        myServer.broadcastMsg("Клиент " + parts[1] + " поменял на ник на " + newNick);
                        myServer.broadcastClientsList();
                    }
                }
                if (str.equals("/clients")) {
                    myServer.broadcastClientsList();
                }
                if (str.equals("/help")) {
                    sendMsg("/clients - для отображения списка клиентов онлайн\n"+
                            "/nc [старый ник] [новый ник]- для смены ника\n"+
                            "/w [ник клиента] [ваше сообщение] - личное сообщение клиенту\n"+
                            "/end - завершить работу клиента ");
                }
                continue;
            }
            myServer.broadcastMsg(name + ": " + str);
        }


    }



    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void closeConnection() {
        myServer.unsubscribe(this);
        myServer.broadcastMsg(name + " вышел из чата");
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
