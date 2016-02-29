
import ecs100.*;
import java.io.*;
import java.net.Socket;
import java.util.*; 

/**
 * Basic IRC Chat Client 
 */

public class ChatClient {
    private String server = "irc.ecs.vuw.ac.nz";  // default IRC server for testing.
    private static final int IRC_PORT = 6667;     // The standard IRC port number.
    Socket socket = null;
    Scanner serverIn;
    PrintStream serverOut;
    String channel = null;
    String requestChannel = null;
    String toSend = null;
    String targetUser = null;
    String currentUsername;

    boolean isSendMessage = false; //checks if it is sending a message

    ArrayList<TextWindow> tw = new ArrayList<TextWindow>(); //creates an array of text windows
    String userMessage;

    /**
     * main: construct a new ChatClient
     */
    public static void main(String[] args) {
        new ChatClient();
    }

    /** 
     * Sets up the user interface.
     */
    public ChatClient (){ 
        UI.addButton("Connect", this::connect);
        UI.addButton("Disconnect", this::closeConnection);
        UI.addButton("Show Channels", this::showChannel);
        UI.addButton("Join a Channel", this::joinChannel);
        UI.addTextField("Enter Channel Name", this::enterChannelName);
        UI.addButton("Leave Channel", this::leaveChannel);
        UI.addButton("Send to Channel", this::sendToChannel);    
        UI.addButton("Send to User", this::sendToUser);
        UI.addTextField("Enter Target User", this::enterTargetUser);
        UI.addTextField("Message Field", this::message);
        UI.addButton("Show Nicknames", this::showNames);
        UI.addButton("Show User Count", this::showUsers);
        UI.addButton("Show Help", this::showHelp);
        UI.addButton("Show the Message of the Day", this::showMOTD);
        UI.addButton("Check for Nickname", this::showISON);
    }

    /**
     * If there is currently an active socket, it should close the
     *  connection and set the socket to null.
     * Creates a socket connected to the server. 
     * Creates a Scanner on the input stream of the socket, 
     *  and a PrintStream on the output stream of the socket.
     * Logs in to the server (calling the loginToServer Message)
     * Once login is successful, starts a separate thread to
     *  listen to the server and process the messages from it.
     */
    public void connect(){
        try{
            if(socket!=null){
                closeConnection();
            }
            socket = new Socket("irc.ecs.vuw.ac.nz", IRC_PORT);
            serverIn = new Scanner(socket.getInputStream());
            serverOut = new PrintStream(socket.getOutputStream());
            if(login()){
                new Thread(new Runnable(){public void run(){
                            listenToServer();
                        }}).start();
                sendToServer();
            }
        }catch(IOException e){
            System.out.println("Failed connection "+ e);
        }

    }

    /**
     * Attempt to log in to the Server and return true if successful, false if not.
     *  Ask user for username and real name
     *  Send info to server (NICK command and USER command)
     *  Read lines from server until get a message containing 004 (success) or
     *   a message containing 433 (failure - nickname in use)
     *  (For debugging, at least, print out all lines from the server)
     */
    private boolean login(){
        String username = UI.askToken("Enter your usercode: ");
        String realname = UI.askString("Enter your real name: ");

        currentUsername = username;

        serverOut.print("NICK " + username + "\r\n");
        serverOut.print("USER " + username + " 0 unused :" + realname +  "\r\n");

        while(serverIn.hasNext()){
            String rcvd = serverIn.nextLine();
            String parsed[] = parseMessage(rcvd);
            String command = parsed[1];
            if(command.equals("004")){//successful login message
                UI.println("Login Successful");
                return true;
            }            
            if(command.equals("433")){//nickname already in use
                UI.println(parsed[4]);
                return false;
            }
        }
        UI.println("Login Failed");
        return false;
    }

    /**
     * Send a message to the current server:
     *  - check that the socket and the serverOut are not null
     *  - print the message with a \r\n at the end to serverOut
     *  - flush serverOut (to ensure the message is sent)
     */
    private void send(String msg, String target){
        if(socket != null && serverOut != null && target !=null){
            serverOut.print("PRIVMSG " + target + " :" + msg + "\r\n");
            tw.get(checkTextWindow(target)).interact("> " + msg);
            serverOut.flush();
        }else{
            UI.print("No conenction to a server");
            return;
        }
    }

    /**
     * Method run in the the thread that is listening to the server.
     * Loop as long as there is anything in the serverIn scanner:
     *   Get and process the next line of input from the scanner
     *   Simple version: 
     *    prints the line out for the user
     *    Checks if the line contains "SQUIT",
     *       if so, close the socket, set serverIn and serverOut set the quit the program.
     *      if the line contains "PING", send a PONG message back
     *        (must be identical to the line, but with "PING" replaced by "PONG")
     *   Better version parses the line into source, command, params, finalParam
     *    (where the source and finalParam are optional, and params may be empty)
     *    Then deals with the message appropriately.
     */
    private void listenToServer() {
        try{
            serverIn = new Scanner(socket.getInputStream());
            if (serverIn != null){
                while (serverIn.hasNext()){
                    String rcvd = (serverIn.nextLine());
                    String parsed[] = parseMessage(rcvd);
                    String command = "-";
                    if (parsed.length > 1){ //stops an array out of bounds error, if there is a problem with the message
                        command = parsed[1];
                    }
                    if(command.equals("SQUIT")){//quits from server
                        closeConnection();
                    }
                    else if(command.equals("QUIT")){//shows users quitting out of the server
                        String un[] = parsed[0].split("!", 2);//cleans up the username
                        un[0] = un[0].replace(":", "");
                        UI.println(un[0] + " Left Server");
                    }
                    else if(command.equals("PING")){//sends out PONG if it receives a PING
                        serverOut.print("PONG" + " :" + server + "\r\n");
                    }
                    else if(command.equals("MODE")){//receiving the mode message
                        //do nothing
                    }
                    else if(command.equals("PART")){//receiving the mode message
                        String un[] = parsed[0].split("!", 2);//cleans up the username
                        un[0] = un[0].replace(":", "");
                        tw.get(checkTextWindow(channel)).interact(un[0] + " Left Channel: " + parsed[parsed.length - 1]);
                        if(un[0].equals(currentUsername)){
                            tw.get(checkTextWindow(channel)).remove();
                            tw.remove(checkTextWindow(channel));
                            channel = null;
                        }
                    }
                    else if(command.equals("JOIN")){//receiving the mode message
                        String un[] = parsed[0].split("!", 2);//cleans up the username
                        un[0] = un[0].replace(":", "");
                        tw.get(checkTextWindow(channel)).interact(un[0] + " Joined Channel: " + parsed[parsed.length - 1]);
                        if(un[0].equals(currentUsername)){//close the channel window
                            tw.get(checkTextWindow(channel));
                        }
                    }
                    else if(command.equals("PRIVMSG")){//shows a private message
                        String un[] = parsed[0].split("!", 2);//cleans up the username
                        un[0] = un[0].replace(":", "");
                        String target = parsed[2];
                        String message = parsed[3].replace(":", "");
                        if(un[0] != null && message != null && tw !=null){
                            if (target.equals(channel)){// the message is to the channel
                                tw.get(checkTextWindow(channel)).interact(un[0] + " - " + message);
                            }else{ //the message is to the user
                                tw.get(checkTextWindow(un[0])).interact(un[0] + " - " + message);
                            }
                        }
                    }
                    else if(command.equals("322")){//shows a list of channels
                        String channel = parsed[3];
                        String userCount = parsed[4];
                        UI.println(channel + " has " + userCount + " user(s)");
                    }
                    else if(command.equals("321")){//start of list of channels
                        //do nothing
                    }
                    else if(command.equals("323")){//end of list of channels
                        //do nothing
                    }
                    else if(command.equals("353")){//shows all of the nicknames
                        UI.println("Nicknames on the channel: " + parsed[parsed.length - 1]);
                    }
                    else if(command.equals("366")){//end of names list
                        //do nothing
                    }
                    else if(command.equals("303")){//checks the nicknmaes on the server
                        if(!isSendMessage){//normal operation
                            if (parsed[parsed.length -1].equals("")){
                                UI.println("There are no matching nicknames");
                            }else{
                                UI.println("That nickname matches one on the server.");
                            }
                        }else{//check if user exists to send message
                            if (parsed[parsed.length -1].equals("")){//if user doesn't exist
                                UI.println("Nickname doesn't exist on server");
                            }else{//if user esits - send message
                                toSend = userMessage;
                                send(toSend, targetUser);
                            }
                            isSendMessage = false;//stops message sending process
                        }
                    }
                    else{//if not specific error type, it prints the extended argument
                        UI.println(parsed[parsed.length - 1]);
                    }
                }
            }
        }catch(IOException e){
            System.out.println("Failed connection "+ e);
        }
    }

    /**
     * Send to server - listens for the user to put in their own inputs
     */
    public void sendToServer(){
        try{
            serverOut = new PrintStream(socket.getOutputStream());
        }catch(IOException e){
            System.out.println("Failed connection "+ e);
        }
    }

    /**
     * count the number of space occurances in the message and then split the
     * message into its according parameters
     */
    public String[] parseMessage(String s){
        int spaceCount = 0;
        int colonCount = 0;
        for(int i = 0; i < s.length(); i++){
            if (s.charAt(i) == ' '){spaceCount ++;}
            if (s.charAt(i) == ':'){colonCount ++;}
            if (colonCount >= 2){ break;}
        }
        String[] newString = s.split(" ", spaceCount + 1);
        newString[newString.length - 1] = newString[newString.length - 1].replace(":", "");
        return newString;
    }

    /**
     * Close the connection:
     *  - close the socket,
     *  - set the serverIn and serverOut to null
     *  - print a message to the user
     */
    public void closeConnection(){
        if(socket != null && serverOut != null){
            try{
                serverOut.print("SQUIT \r\n");
                socket.close();
                serverIn.close();
                serverIn = null;
                serverOut.close();
                serverOut = null;
                UI.println("Connection Closed");
            }catch(IOException e){
                System.out.println("Failed disconnection "+ e);
            }
        }
    }

    /**
     * shows all of the channels on the server
     */
    public void showChannel(){
        if (serverIn !=null && serverOut != null){
            serverOut.print("LIST \r\n");
        }else{
            UI.println("Must be connected to server.");
        }
    }

    /**
     * allows the user to choose a channel to join
     */
    public void joinChannel(){
        if (serverIn !=null && serverOut != null){
            serverOut.print("JOIN " + requestChannel + "\r\n");
            channel = requestChannel;
        }else{
            UI.println("Must be connected to server.");
        }
    }

    /**
     * has the user leave the channel if they are onto to one
     */
    public void leaveChannel(){
        if (serverIn !=null && serverOut != null){
            if(channel!=null){
                serverOut.print("PART " + channel + "\r\n");
            }else{
                UI.println("Not joined to a channel");
            }
        }else{
            UI.println("Must be connected to server.");
        }
    }

    /**
     * Makes the name of the channel value being entered
     */
    private void enterChannelName(String c){
        requestChannel = c;
    }

    /**
     * Sends the text entered to the channel that the user is logged onto
     */
    public void sendToChannel(){
        if(serverIn !=null && serverOut != null){
            if(channel != null){
                toSend = userMessage;
                send(toSend, channel);
            }
        }else{
            UI.println("Must be connected to server.");
        }
    }

    /**
     * Sends the text entered to the user on the same server between them
     */
    public void sendToUser(){
        if(serverIn !=null && serverOut != null){
            if(targetUser != null){
                isSendMessage = true;
                serverOut.print("ISON " + targetUser + "\r\n"); //checks if user exists
            }
        }else{
            UI.println("Must be connected to server.");
        }
    }

    /**
     * Makes the name of the target user to be entered equal whats in the textfield
     */
    private void enterTargetUser(String t){
        targetUser = t;
    }

    /**
     * Is the field that the user types in the message they have
     */
    private void message(String m){
        userMessage = m;
    }

    /**
     * checks through all of the text window occurances to see if they equal
     * the channel/target it's being sent to. If not, a new one is made and
     * it returns that index
     */
    public int checkTextWindow(String target){
        int index = 0;
        for (index = 0; index < tw.size(); index ++){
            if(tw.get(index).equals(target)){return index;}
        }
        tw.add(new TextWindow(target));
        return index;
    }

    /**
     * show all of the nicknames on the channel that the user is connected to
     */
    public void showNames(){
        if(serverIn !=null && serverOut != null){
            if (channel != null){
                serverOut.print("NAMES " + "\r\n");
            }else{
                UI.println("You need to be connected to a channel before seeing users on it");
            }
        }else{
            UI.println("Must be connected to server.");
        }
    }

    /**
     * show all of the usernames on the server that the user is connected to
     */
    public void showUsers(){
        if(serverIn !=null && serverOut != null){
            serverOut.print("USERS " + "\r\n");
        }else{
            UI.println("Must be connected to server.");
        }
    }

    /**
     * shows the help file on the server, helping out the user understand IRC
     */
    public void showHelp(){
        if(serverIn !=null && serverOut != null){
            serverOut.print("HELP " + "\r\n");
        }else{
            UI.println("Must be connected to server.");
        }
    }

    /**
     * sends message to the server asking for the MOTD
     */
    public void showMOTD(){
        if(serverIn !=null && serverOut != null){
            serverOut.print("MOTD " + "\r\n");
        }else{
            UI.println("Must be connected to server.");
        }
    }

    /**
     * sends message to the server asking for the MOTD
     */
    public void showISON(){
        if(serverIn !=null && serverOut != null){
            String s = UI.askString("Check for nicknames on server:");
            serverOut.print("ISON " + s + "\r\n");
        }else{
            UI.println("Must be connected to server.");
        }
    }
}
