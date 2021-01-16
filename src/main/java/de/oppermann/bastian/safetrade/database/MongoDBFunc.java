package de.oppermann.bastian.safetrade.database;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class MongoDBFunc {

    /**
     * Created by Chiharu-Hagihara
     * Reference by takatronix:MySQLFunc
     */

    private JavaPlugin plugin;
    String HOST;
    String USER;
    String PASS;
    int PORT;
    String DATABASE;
    String CUSTOM;
    private MongoClient con = null;

    public MongoDBFunc( String HOST, String USER, String PASS, int PORT, String DATABASE, String CUSTOM) {
        this.HOST = HOST;
        this.USER = USER;
        this.PASS = PASS;
        this.PORT = PORT;
        this.DATABASE = DATABASE;
        this.CUSTOM = CUSTOM;
    }

    public MongoClient open() {
        try {
            // mongodb://user1:pwd1@host1/?authSource=db1
            MongoClientURI uri = new MongoClientURI("mongodb://" + USER + ":" + PASS + "@" + HOST + ":" + PORT + "/" + DATABASE + CUSTOM);
            this.con = new MongoClient(uri);
            return this.con;
        } catch (Exception var2) {
            plugin.getLogger().log(Level.SEVERE, "Could not connect to MySQL server, error code: " + var2);
        }
        return this.con;
    }

    public boolean checkConnection() {
        return this.con != null;
    }

    public void close(MongoClient c) {
        c = null;
    }

    public MongoClient getCon() {
        return this.con;
    }

    public void setCon(MongoClient con) {
        this.con = con;
    }
}