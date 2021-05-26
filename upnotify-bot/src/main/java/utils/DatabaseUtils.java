package utils;


import objects.Request;
import objects.Snapshot;
import objects.User;
import org.openqa.selenium.devtools.database.Database;
import upnotify_bot.UpnotifyBot;

// import javax.imageio.ImageIO;
// import javax.xml.crypto.Data;

// import java.awt.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.sql.*;
import java.util.ArrayList;

import javax.imageio.ImageIO;

interface DatabaseUtilsInterface {
	/**
	 * This function is going to insert a user if said user is not present yet
	 * 
	 * This function will be called by UpdateReceiver whenever an update is received, so it must be somewhat efficient
	 * 
	 * if user not exists, creates it as well.
	 * 
	 * Level of user is default level on creation
	 * 
	 * @return reference to User instance
	 */
	public User retrieveUserFromId(long userId, String userName);
	
	
	/**
	 * This function returns the list of all requests from our database.
	 * 
	 * Function will be called from Main.java when the code is run for the first time, so that the requests that are already present will be submitted to the UpnotifyReceiver. 
	 * 
	 * 
	 * 
	 * @return reference to list of Request instances
	 */
	public ArrayList<Request> getRequests();	
	
	/**
	 * 
	 * @param snapshotId
	 * @return snapshot with resp. Id
	 */
	public Snapshot retrieveSnapshotFromId(int snapshotId);
	
	
}

/**
 * @todo Implement DatabaseUtilsInterface'
 * @todo check multithreading capabilities
 */
public class DatabaseUtils implements DatabaseUtilsInterface
{
    public Connection connection = null;
    //public String url = "jdbc:sqlite:upnotify-bot/src/main/resources/upnotify.db"; // for vscode
    //public String url = "jdbc:sqlite:src/main/resources/upnotify.db"; // for eclipse
    public String url = "jdbc:sqlite:" + this.getClass().getResource("/upnotify.db");
    private static DatabaseUtils single_instance = null;

    public static DatabaseUtils getDatabaseUtils() {
        if (single_instance == null) {
            single_instance = new DatabaseUtils();
            System.out.println("Instance of 'DatabaseUtils' has been created");
            DatabaseUtils.getDatabaseUtils().createTables();
        }
        
        return single_instance;
    
    }

    private DatabaseUtils(){
    }

    public void buildConnection(){
    	System.out.println("Connecting to db");
        try {
            connection = DriverManager.getConnection(url);
        }
        catch(SQLException e){
            //System.out.println("there is a problem with db connection");
            e.printStackTrace();
        }
        
    }

    public void closeConnection(){
        try
        {
            if(connection != null)
                connection.close();
        }
        catch(SQLException e)
        {
            // connection close failed.
            e.printStackTrace();
        }
    }



    //tablo dbde var ise true yok ise false döndüren bir fonksiyon
    private boolean tableExists(String tableName,Connection conn){
        try{
            DatabaseMetaData md = conn.getMetaData();
            ResultSet rs = md.getTables(null, null, tableName, null);
            if(rs.next()){
                //table exists
                return true;
            }
            else{
                return false;
            }
        }catch(SQLException e){
            e.printStackTrace();
            return true;
        }
    }

    public void createTables(){
            buildConnection();
            try{
                Statement statement = connection.createStatement();
                statement.setQueryTimeout(30);  // set timeout to 30 sec.
                
                
                // yok ise USERS tablosunu oluştur
                if(!tableExists("USER",connection)){
                    String create_user_table = "create table USER\n" +
                            "(\n" +
                            "\ttelegramId INTEGER\n" +
                            "\t\tconstraint USER_pk\n" +
                            "\t\t\tprimary key autoincrement,\n" +
                            "\tcheckLevel int default 3,\n" +
                            "\tuserName String\n" +
                            ");\n" +
                            "\n" +
                            "create unique index USER_telegramId_uindex\n" +
                            "\ton USER (telegramId);\n" +
                            "\n" ;

                    try {
                        statement.executeUpdate(create_user_table);
                        System.out.print("USER table has created");
                    } catch (SQLException e){
                        e.printStackTrace();
                    }
                }
                else{

                    System.out.println("USER table already exists");
                    //USERS table allready exists

                }
                // yok ise WEB_PAGES tablosunu oluştur
                if(!tableExists("SNAPSHOT",connection)){
                    String create_webpages_table = "create table SNAPSHOT\n" +
                            "(\n" +
                            "\tsnapshotId INTEGER not null\n" +
                            "\t\tconstraint SNAPSHOT_pk\n" +
                            "\t\t\tprimary key autoincrement,\n" +
                            "\turl String,\n" +
                            "\tscreenshot BLOB,\n" +
                            "\tsiteContentHash String\n" +
                            ");\n" +
                            "\n" +
                            "create unique index SNAPSHOT_snapshotId_uindex\n" +
                            "\ton SNAPSHOT (snapshotId);\n";

                    try {
                        statement.executeUpdate(create_webpages_table);
                        System.out.print("SNAPSHOT table has created");
                    } catch (SQLException e){
                        e.printStackTrace();
                    }
                }else{

                    System.out.println("SNAPSHOT table already exists");
                    //WEB_PAGES table allready exists

                }

                // yok ise WEB_PAGES tablosunu oluştur
                if(!tableExists("REQUEST",connection)){
                    String create_requests_table = "create table REQUEST\n" +
                            "(\n" +
                            "    requestId     INTEGER\n" +
                            "        constraint REQUEST_pk\n" +
                            "            primary key autoincrement,\n" +
                            "    telegramId    int\n" +
                            "        references USER,\n" +
                            "    snapshotId    int,\n" +
                            "        references SNAPSHOT\n" +
                            "    checkInterval int,\n" +
                            "   isActive INTEGER, \n"+
                            "    lastCheckUnix int\n" +
                            ");\n" +
                            "\n" +
                            "create unique index REQUEST_requestId_uindex\n" +
                            "    on REQUEST (requestId);\n" +
                            "\n";


                    try {
                        statement.executeUpdate(create_requests_table);
                        System.out.print("REQUEST table has created");
                    } catch (SQLException e){
                        e.printStackTrace();
                    }
                }else{
                    System.out.println("REQUEST table already exists");
                    //WEB_PAGES table allready exists
                }

            }catch (SQLException e){
                e.printStackTrace();
            }


            closeConnection();

        }

        //Select all users from USER table and return a user list
        private ArrayList<User> selectUsers(){

            ArrayList<User> userList = new ArrayList<User>();
            buildConnection();
            try{
                Statement statement = connection.createStatement();
                statement.setQueryTimeout(30);  // set timeout to 30 sec.

                String selectQuery = "SELECT * FROM USER";
                ResultSet rs = statement.executeQuery(selectQuery);
                while (rs.next()) {
                    User selectedUser = new User(rs.getLong("telegramId"),
                            rs.getInt("checkLevel"), rs.getString("userName"));
                    userList.add(selectedUser);
                }
            }catch(SQLException e){
                e.printStackTrace();
            }
            closeConnection();
            return userList;

        }

        //select a user with a specific telegramId
        private User retrieveUserFromId(Long telegramId){
            User selectedUser = new User();
            buildConnection();
            try{
                Statement statement = connection.createStatement();
                statement.setQueryTimeout(30);  // set timeout to 30 sec.

                String selectFromIdQuery = String.format("SELECT * FROM USER\n" +
                        "WHERE USER.telegramId = %d;",telegramId);
                ResultSet rs = statement.executeQuery(selectFromIdQuery);

                selectedUser.telegramId = rs.getLong("telegramId");
                selectedUser.userName = rs.getString("userName");
                selectedUser.checkLevel = rs.getInt("checkLevel");

                rs.close();
                statement.close();

            }catch(SQLException e){
                e.printStackTrace();
            }
            finally {
                closeConnection();
            }

            return selectedUser;

        }

        // insert a user into USER table
        private void insertUser(Long telegramId,int checkLevel, String userName){
            buildConnection();
            try{
                Statement statement = connection.createStatement();
                statement.setQueryTimeout(30);  // set timeout to 30 sec.

                String insertQuery = String.format("INSERT INTO USER(" +
                        "telegramId,checkLevel,userName)\n"+
                        "VALUES(%d,%d,'%s');",telegramId,checkLevel,userName);

                statement.executeQuery(insertQuery);
                statement.close();

            }catch(SQLException e){
                e.printStackTrace();
            }
            finally {
                closeConnection();
            }


        }

        private int insertSnapshot(String url, InputStream screenshot, String siteContentHash){
            buildConnection();
            int generatedKey = -1;
            try{
                String insertSnapshotQ= "INSERT INTO SNAPSHOT(url,screenshot,siteContentHash)" + "VALUES(?,?,?)";
                System.out.println(111);
                PreparedStatement ps = connection.prepareStatement(insertSnapshotQ,Statement.RETURN_GENERATED_KEYS);
                System.out.println(112);
                ps.setString(1,url);
                System.out.println(113);
                
                if (screenshot == null) {
                	ps.setNull(2, Types.NULL );
                	System.out.println(114);
                } else {
                	//ps.setBlob(2, screenshot);
                	//ps.setBinaryStream(2,screenshot);
                    //byte screenshotByte[] = ImageUtils.getImageUtils().getByteData(ImageUtils.getImageUtils().convertInputStreamIntoBufferedImage(screenshot));
                	ps.setBytes(2, screenshot.readAllBytes());
                	System.out.println(222);
                }
                ps.setString(3,siteContentHash);
                System.out.println(115);
                ps.executeUpdate();
                System.out.println(116);

                ResultSet genKeys = ps.getGeneratedKeys();
                if ( genKeys.next() ) {
                    generatedKey= genKeys.getInt( 1 );
                } else {
                    System.out.println("there is no generated id");
                }

                ps.close();


            }
            catch(SQLException | IOException e){
            	System.out.println(117);
                e.printStackTrace();
            }
            finally {
                closeConnection();
                System.out.println(119);
                return generatedKey;
            }

        }


        private InputStream retrieveImageInputStreamFromSnapshotId(int SnapshotId){
            buildConnection();
            InputStream is = null;
            try{
                Statement statement = connection.createStatement();
                String retrieveSnapshot = String.format("SELECT screenshot FROM SNAPSHOT" +
                        "WHERE SnapshotId = %d",SnapshotId);
                ResultSet rs = statement.executeQuery(retrieveSnapshot);
                Blob ablob = rs.getBlob("screenshot");
                is = ablob.getBinaryStream();

            }catch(SQLException e){
                e.printStackTrace();
            }
            finally {
                closeConnection();
            }

        return is;
        }

/*
        private void insertRequest(Long telegramId,String userName, int checkInterval,String url,InputStream screenshot,
                                  String siteContentHash){

            buildConnection();
            try{
                Statement statement = connection.createStatement();
                statement.setQueryTimeout(30);  // set timeout to 30 sec.


                // insert user if not exists
                User checkUser = retrieveUserFromId(telegramId);
                if(checkUser.userName == null){
                    insertUser(telegramId,checkInterval,userName);
                }


                // insert snapshot and get the id
                insertSnapshot(url,screenshot,siteContentHash);
                int snapshotId = statement.executeQuery("SELECT last_insert_rowid()").getInt(0);

                // insert Request
                int lastCheckedUnix = 100;
                String insertReqQuery = String.format("INSERT INTO REQUEST" +
                        "(telegramId,snapshotId,checkInterval,lastCheckedUnix) VALUES" +
                        "(%d,%d,%d,%d)",telegramId,snapshotId,checkInterval,lastCheckedUnix);
                statement.executeQuery(insertReqQuery);


            }catch(SQLException e){
                System.err.println(e.getMessage());
            }

        } */

        private boolean checkUserExists(Long telegramId){
            buildConnection();
            try{
                boolean exists;
                Statement statement = connection.createStatement();
                String checkquery = String.format("SELECT * FROM" +
                        " USER WHERE USER.telegramId = %d",telegramId);
                ResultSet rs = statement.executeQuery(checkquery);
                if(rs.next()){
                    exists = true;
                }
                else{
                    exists = false;
                }
                closeConnection();
                return exists;

            }catch(SQLException e){
                e.printStackTrace();
                closeConnection();
                return false;
            }

        }


    @Override
    public User retrieveUserFromId(long userId, String userName) {
        //check if user exists
        if(!checkUserExists(userId)){
            //create if not
            int checkLevel = Config.getConfig().DEFAULT_LEVEL;
            insertUser(userId,checkLevel,userName);
        }

        User myUser = retrieveUserFromId(userId);

        //return user
        return myUser;
    }

    @Override
    public ArrayList<Request> getRequests() {
        ArrayList<Request> reqList = new ArrayList<Request>();

        buildConnection();
        try{
            Statement statement = connection.createStatement();
            String selectReqs = "SELECT * FROM REQUEST";
            ResultSet rs = statement.executeQuery(selectReqs);
            while(rs.next()){
                Request myReq = new Request(rs.getInt("requestId"),rs.getLong("telegramId")
                ,rs.getInt("snapshotId"),rs.getInt("checkInterval"),rs.getLong("lastCheckUnix"),
                        rs.getBoolean("isActive"));
                reqList.add(myReq);
            }

        }catch(SQLException e){
            e.printStackTrace();
        }
        closeConnection();
        return reqList;

    }

    @Override
    public Snapshot retrieveSnapshotFromId(int snapshotId) {
        Snapshot mySnapshot = new Snapshot();
        buildConnection();
        try{
            Statement statement = connection.createStatement();
            String getSnapshotQ = String.format("SELECT * FROM SNAPSHOT" +
                    " WHERE SNAPSHOT.snapshotId = %d",snapshotId);
            ResultSet rs = statement.executeQuery(getSnapshotQ);
            mySnapshot.snapshotId = rs.getInt("snapshotId");
            mySnapshot.url = rs.getString("url");
//<<<<<<< development
            //mySnapshot.screenshot = ImageUtils.getImageUtils().convertInputStreamIntoBufferedImage(rs.getBinaryStream("screenshot"));
            //mySnapshot.screenshot = ImageUtils.getImageUtils().convertInputStreamIntoBufferedImage(rs.getBytes("screenshot"));
            mySnapshot.screenshot = ImageUtils.getImageUtils().convertInputStreamIntoBufferedImage(rs.getBinaryStream("screenshot"));
            
            
            // =======
//             Blob blob = rs.getBlob("screenshot");
//             try {
//                 mySnapshot.screenshot = ImageIO.read(blob.getBinaryStream());
//             } catch (IOException e) {
//                 e.printStackTrace();
//             }
// >>>>>>> development
            mySnapshot.siteContentHash = rs.getString("siteContentHash");

        }catch(SQLException e){
            e.printStackTrace();
        }
        closeConnection();
        return mySnapshot;

    }

    public objects.Request retrieveRequestFromId(int requestId) {
        Request myRequest = new Request();
        buildConnection();
        try{
            Statement statement = connection.createStatement();
            String getRequestQ = String.format("SELECT * FROM REQUEST" +
                    " WHERE REQUEST.requestId = %d",requestId);
            ResultSet rs = statement.executeQuery(getRequestQ);
            myRequest.requestId = rs.getInt("requestId");
            myRequest.snapshotId = rs.getInt("snapshotId");
            myRequest.checkInterval = rs.getInt("checkInterval");
            myRequest.telegramId = rs.getLong("telegramId");
            myRequest.isActive = rs.getBoolean("isActive");
            myRequest.lastCheckedUnix = rs.getLong("lastCheckUnix");

        }catch(SQLException e){
            e.printStackTrace();
        }
        closeConnection();
        return myRequest;
    }

	// Requests.telegramId,   Requests.LastCheckUnix, Snapshot.url, Snapshot.screenshot, Snapshot.siteContentHash
	public boolean addRequest(Long chatId, long epochSecond, String url2, BufferedImage screenshot,
			String siteContentHash) {
        int generatedKey = -1;
		
        try{
            // insert snapshot and get the id
            int snapshotId= insertSnapshot(url2,ImageUtils.getImageUtils().convertBufferedImageIntoInputStream(screenshot),siteContentHash);
            System.out.println("Inserted snapshot");

            boolean isActive = true;
            int isActiveInt = (isActive)? 1 : 0;

            System.out.println("Got snapshot id: " + snapshotId);

            //statement.setQueryTimeout(30);  // set timeout to 30 sec.
            String insertReqUpdate = String.format("INSERT INTO REQUEST" +
                    "(telegramId,snapshotId,checkInterval,lastCheckUnix,isActive) VALUES" +
                    "(%d,%d,%d,%d,%d)",chatId,snapshotId,Config.getConfig().DEFAULT_LEVEL, epochSecond,isActiveInt);
            buildConnection();
            PreparedStatement statement = connection.prepareStatement(insertReqUpdate, Statement.RETURN_GENERATED_KEYS);



            statement.executeUpdate();
            System.out.println("Inserted Request");


            ResultSet genKeys = statement.getGeneratedKeys();
            if ( genKeys.next() ) {
                generatedKey= genKeys.getInt( 1 );
            } else {
                System.out.println("there is no generated id");
            }

            objects.Request req = retrieveRequestFromId(generatedKey);
            MultiprocessingUtils.getMultiProcessingUtils().submitUpnotify(UpnotifyBot.getUpnotifyBot(), req);

        }catch(Exception e){
            e.printStackTrace();
            closeConnection();
            return false;
        }

		closeConnection();

		return true;
		
	}
	
	public boolean editSnapshot(Snapshot snap) {
		buildConnection();
        
        try{
            String editSnapshotQ = "UPDATE SNAPSHOT SET url = ? , "
            						+ "screenshot = ? , "
            						+ "siteContentHash = ? "
            						+ "WHERE snapshotId = ?";
           
            PreparedStatement ps = connection.prepareStatement(editSnapshotQ);
            ps.setString(1, snap.url);
            
            if (snap.screenshot == null) {
            	ps.setNull(2, Types.NULL);
            } else {
            	ps.setBytes(2, ImageUtils.getImageUtils().convertBufferedImageIntoInputStream(snap.screenshot).readAllBytes());
            }
            ps.setString(3, snap.siteContentHash);
            ps.setInt(4, snap.snapshotId);
            ps.executeUpdate();
            ps.close();
        }
        catch(SQLException | IOException e){
            e.printStackTrace();
            closeConnection();
    		return false;
        }
        
		closeConnection();
		return true;
	}

	public boolean editRequest(Request req, Snapshot snap) {
		buildConnection();
		boolean success = false;
		try{
			//I didn't remove try catch blocks since checkInterval can made to be changed in future versions
			
            /*Statement statement = connection.createStatement();
            

            boolean isActive = true;
            int isActiveInt = (isActive)? 1 : 0;
             */
			
			System.out.println("Snapshot id is being edited: " + snap.snapshotId);
			success = editSnapshot(snap);

            /*String updateSnapQ = String.format("INSERT INTO REQUEST" +
                    "(telegramId,snapshotId,checkInterval,lastCheckUnix,isActive) VALUES" +
                    "(%d,%d,%d,%d,%d)",0,0,0,0);
            statement.executeUpdate(updateSnapQ);
            */
           
            
        }catch(Exception e){
        	System.err.println(e.getMessage());
            closeConnection();
            return false;
        }

		System.out.println("Edited Request, id " + req.requestId);
		closeConnection();
		return success;
	}

	public boolean removeRequest(Request req) {
        buildConnection();
        try {
            String removeRequest = "DELETE FROM REQUEST WHERE requestId = ?";
            PreparedStatement ps = connection.prepareStatement(removeRequest);
            ps.setInt(1, req.requestId);
            ps.executeUpdate(removeRequest);

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        closeConnection();
        return true;
    }

    public boolean removeUser(User user) {
        buildConnection();
        try {
            String removeUser = "DELETE FROM USER WHERE telegramId = ?";
            PreparedStatement ps = connection.prepareStatement(removeUser);
            ps.setLong(1, user.telegramId);
            ps.executeUpdate(removeUser);

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        closeConnection();
        return true;
    }

    public boolean removeSnapshot(Snapshot ss) {
        buildConnection();
        try {
            String removeSnapshot = "DELETE FROM SNAPSHOT WHERE snapshotId = ?";
            PreparedStatement ps = connection.prepareStatement(removeSnapshot);
            ps.setLong(1, ss.snapshotId);
            ps.executeUpdate(removeSnapshot);

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        closeConnection();
        return true;
    }

	public boolean removeAllRequests() {
        buildConnection();
        try {
            Statement statement = connection.createStatement();

            String removeRequest = "DELETE FROM REQUEST WHERE 1=1";
            statement.executeUpdate(removeRequest);

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        closeConnection();
        return true;

    }

    public boolean removeAllUsers() {
        buildConnection();
        try {
            Statement statement = connection.createStatement();

            String removeUser = "DELETE FROM USER WHERE 1=1";
            statement.executeUpdate(removeUser);

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        closeConnection();
        return true;

    }

    public boolean removeAllSnapshots() {
        buildConnection();
        try {
            Statement statement = connection.createStatement();

            String removeSnapshot = "DELETE FROM SNAPSHOT WHERE 1=1";
            statement.executeUpdate(removeSnapshot);

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        closeConnection();
        return true;
    }

    public boolean cleanDatabase() {
        buildConnection();
        try {
            Statement statement = connection.createStatement();

            String removeSnapshot = "DELETE FROM SNAPSHOT WHERE 1=1";
            String removeUser = "DELETE FROM USER WHERE 1=1";
            String removeRequest = "DELETE FROM REQUEST WHERE 1=1";

            statement.executeUpdate(removeSnapshot);
            statement.executeUpdate(removeRequest);
            statement.executeUpdate(removeUser);

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        closeConnection();
        return true;
    }

    public boolean dropTables() {
        buildConnection();
        try {
            Statement statement = connection.createStatement();

            String dropUserTable = "DROP TABLE USER";
            String dropSnapshotTable = "DROP TABLE SNAPSHOT";
            String dropRequestTable = "DROP TABLE REQUEST";
            statement.executeUpdate(dropUserTable);
            statement.executeUpdate(dropSnapshotTable);
            statement.executeUpdate(dropRequestTable);

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        closeConnection();
        return true;
    }
}
