package de.oppermann.bastian.safetrade.database;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class MongoDBManager implements AutoCloseable {

    private JavaPlugin plugin;
    public String HOST;
    public String USER;
    public String PASS;
    public int PORT;
    public String DATABASE;
    public String CUSTOM;
    private boolean connected = false;
    private MongoCollection<Document> coll;
    private MongoClient con = null;
    private MongoDBFunc MongoDB;

    /**
     * Created by Chiharu-Hagihara
     * Reference by takatronix:MySQLManager
     */

    ////////////////////////////////
    //      Constructor
    ////////////////////////////////
    public MongoDBManager(JavaPlugin plugin, String coll) {
        this.plugin = plugin;

        loadConfig();

        this.connected = false;
        this.connected = Connect();
        this.coll = con.getDatabase(DATABASE).getCollection(coll);

        if(!this.connected) {
            this.plugin.getLogger().info("Unable to establish a MongoDB connection.");
        }
    }

    /////////////////////////////////
    //       Load YAML
    /////////////////////////////////
    public void loadConfig(){
        plugin.getLogger().info("MongoDB Config loading");

        plugin.reloadConfig();
        HOST = plugin.getConfig().getString("mongo.host");
        USER = plugin.getConfig().getString("mongo.user");
        PASS = plugin.getConfig().getString("mongo.pass");
        PORT = plugin.getConfig().getInt("mongo.port");
        CUSTOM = plugin.getConfig().getString("mongo.uri");
        DATABASE = plugin.getConfig().getString("mongo.db");

        plugin.getLogger().info("Config loaded");

    }

    ////////////////////////////////
    //       Connect
    ////////////////////////////////
    public Boolean Connect() {
        this.MongoDB = new MongoDBFunc(HOST, USER, PASS, PORT, DATABASE, CUSTOM);
        this.con = this.MongoDB.open();
        if(this.con == null){
            plugin.getLogger().info("failed to open MongoDB");
            return false;
        }

        try {
            this.connected = true;
            this.plugin.getLogger().info("Connected to the database.");
        } catch (Exception var6) {
            this.connected = false;
            this.plugin.getLogger().info("Could not connect to the database.");
        }

        this.MongoDB.close(this.con);
        return this.connected;
    }

    ////////////////////////////////
    //       InsertOne Query
    ////////////////////////////////
    public void queryInsertOne(String doc) {
        coll.insertOne(Document.parse(doc));
    }

    ////////////////////////////////
    //       UpdateOne Query
    ////////////////////////////////
    public void queryUpdateOne(String filterKey, String filterValue, String updateKey, String updateValue) {
        Document filterdoc = new Document();
        Document updatedoc = new Document();
        Document update = new Document();
        filterdoc.append(filterKey, filterValue);
        updatedoc.append(updateKey, updateValue);
        update.append("$set", updatedoc);

        coll.updateOne(filterdoc, update);
    }

    ////////////////////////////////
    //       DeleteOne Query
    ////////////////////////////////
    public void queryDelete(String filterKey, String filterValue) {
        coll.deleteOne(Filters.eq(filterKey, filterValue));
    }

    ////////////////////////////////
    //       Find Query
    ////////////////////////////////
    public List<Document> queryFind(String key, String value) {
        BasicDBObject query = new BasicDBObject(key, value);
        return coll.find(query).into(new ArrayList<>());
    }

    ////////////////////////////////
    //       Count Query
    ////////////////////////////////
    public long queryCount() {
        return coll.countDocuments();
    }

    ////////////////////////////////
    //       Connection Close
    ////////////////////////////////
    @Override
    public void close(){

        try {
            this.con.close();
            this.MongoDB.close(this.con);

        } catch (Exception var4) {
        }

    }

    ////////////////////////////////
    //       Setup BlockingQueue
    ////////////////////////////////
    static LinkedBlockingQueue<String> blockingQueue = new LinkedBlockingQueue<>();

    public static void setupBlockingQueue(JavaPlugin plugin, String conName) {
        new Thread(() -> {
            MongoDBManager mongo = new MongoDBManager(plugin, conName);
            try {
                while (true) {
                    String take = blockingQueue.take();
                    mongo.queryInsertOne(take);
                }
            }catch (Exception e) {
            }
        }).start();
    }

    public static void executeQuery(String query) {
        blockingQueue.add(query);
    }
}