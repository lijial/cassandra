import com.datastax.driver.core.*;

/**
 * Created by lijialiang on 2017/7/31.
 */
public class CassandraTest {

    public Cluster cluster;

    public Session session;

    //连接cassandra
    public void connect() {
        // addContactPoints:cassandra节点ip withPort:cassandra节点端口 默认9042
        // withCredentials:cassandra用户名密码 如果cassandra.yaml里authenticator：AllowAllAuthenticator 可以不用配置
        cluster = Cluster.builder().addContactPoints("127.0.0.1").withPort(9042).build();
//                .withCredentials("cassandra", "cassandra").build();
        session = cluster.connect();
    }

    //驱动里自带了cassandra连接池的配置，将上面的稍作修改
    public void connect_2() {
        PoolingOptions poolingOptions = new PoolingOptions();
        // 每个连接的最大请求数 2.0的驱动好像没有这个方法
        poolingOptions.setMaxRequestsPerConnection(HostDistance.LOCAL, 32);
        // 表示和集群里的机器至少有2个连接 最多有4个连接
        poolingOptions.setCoreConnectionsPerHost(HostDistance.LOCAL, 2).setMaxConnectionsPerHost(HostDistance.LOCAL, 4)
                .setCoreConnectionsPerHost(HostDistance.REMOTE, 2).setMaxConnectionsPerHost(HostDistance.REMOTE, 4);

        // addContactPoints:cassandra节点ip withPort:cassandra节点端口 默认9042
        // withCredentials:cassandra用户名密码 如果cassandra.yaml里authenticator：AllowAllAuthenticator 可以不用配置
        cluster = Cluster.builder().addContactPoints("192.168.3.89").withPort(9042)
                .withCredentials("cassandra", "cassandra").withPoolingOptions(poolingOptions).build();
        // 建立连接
        // session = cluster.connect("test");连接已存在的键空间
        session = cluster.connect();
    }

    /**
     * 创建键空间和表（这2个最好在搭建cassandra的时候完成）
     * 创建键空间
     */
    public void createKeyspace() {
        // 单数据中心 复制策略 ：1
        String cql = "CREATE KEYSPACE if not exists mydb WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'}";
        session.execute(cql);
    }

    /**
     * 创建表
     */
    public void createTable() {
        // a,b为复合主键 a：分区键，b：集群键
        String cql = "CREATE TABLE if not exists mydb.test (a text,b int,c text,d int,PRIMARY KEY (a, b))";
        session.execute(cql);
    }

    /**
     * 对test表的CURD操作,因为这里有mydb.test，所以既有keyspace名，又有表名，不需要别的地方指定了
     * 插入
     */
    public void insert() {
        String cql = "INSERT INTO mydb.test (a , b , c , d ) VALUES ( 'a2',4,'c2',6);";
        session.execute(cql);
    }

    /**
     * 修改
     */
    public void update() {
        // a,b是复合主键 所以条件都要带上，少一个都会报错，而且update不能修改主键的值，这应该和cassandra的存储方式有关
        String cql = "UPDATE mydb.test SET d = 1234 WHERE a='aa' and b=2;";
        // 也可以这样 cassandra插入的数据如果主键已经存在，其实就是更新操作
        String cql2 = "INSERT INTO mydb.test (a,b,d) VALUES ( 'aa',2,1234);";
        // cql 和 cql2 的执行效果其实是一样的
        session.execute(cql);
    }

    /**
     * 删除
     */
    public void delete() {
        // 删除一条记录里的单个字段 只能删除非主键，且要带上主键条件
        String cql = "DELETE d FROM mydb.test WHERE a='aa' AND b=2;";
        // 删除一张表里的一条或多条记录 条件里必须带上分区键
        String cql2 = "DELETE FROM mydb.test WHERE a='aa';";
        session.execute(cql);
        session.execute(cql2);
    }

    /**
     * 查询
     */
    public void query() {
        String cql = "SELECT * FROM mydb.test;";
        String cql2 = "SELECT a,b,c,d FROM mydb.test;";

        ResultSet resultSet = session.execute(cql);
        System.out.print("这里是字段名：");
        for (ColumnDefinitions.Definition definition : resultSet.getColumnDefinitions()) {
            System.out.print(definition.getName() + " ");
        }
        System.out.println();
        System.out.println(String.format("%s\t%s\t%s\t%s\t\n%s", "a", "b", "c", "d",
                "--------------------------------------------------------------------------"));
        for (Row row : resultSet) {
            System.out.println(String.format("%s\t%d\t%s\t%d\t", row.getString("a"), row.getInt("b"),
                    row.getString("c"), row.getInt("d")));
        }
    }

    public static void main(String args[]) {
        CassandraTest cassandraTest = new CassandraTest();
        cassandraTest.connect();
//        cassandraTest.createKeyspace();
//        cassandraTest.createTable();
//        cassandraTest.insert();
//        cassandraTest.delete();
//        cassandraTest.update();
//        cassandraTest.query();
        String cql = "UPDATE mydb.test SET d = 12345 WHERE a='aa' and b=2;";
        cassandraTest.session.execute(cql);
        cql = "DELETE d FROM mydb.test WHERE a='aa' AND b=2;";
        cassandraTest.session.execute(cql);
        cassandraTest.query();
    }
}
