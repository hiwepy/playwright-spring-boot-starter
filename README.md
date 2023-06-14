# canal-spring-boot-starter

#### 组件简介

> 基于 [Canal ](https://github.com/alibaba/canal) 整合的 Starter

> canal [kə'næl]，译意为水道/管道/沟渠，主要用途是基于 MySQL 数据库增量日志解析，提供增量数据订阅和消费

早期阿里巴巴因为杭州和美国双机房部署，存在跨机房同步的业务需求，实现方式主要是基于业务 trigger 获取增量变更。从 2010 年开始，业务逐步尝试数据库日志解析获取增量变更进行同步，由此衍生出了大量的数据库增量订阅和消费业务。

基于日志增量订阅和消费的业务包括

- 数据库镜像
- 数据库实时备份
- 索引构建和实时维护(拆分异构索引、倒排索引等)
- 业务 cache 刷新
- 带业务逻辑的增量数据处理

> 当前的 canal 支持源端 MySQL 版本包括 5.1.x , 5.5.x , 5.6.x , 5.7.x , 8.0.x

#### 使用说明

##### 1、Spring Boot 项目添加 Maven 依赖

``` xml
<dependency>
	<groupId>com.github.hiwepy</groupId>
	<artifactId>canal-spring-boot-starter</artifactId>
	<version>${project.version}</version>
</dependency>
```

##### 2、使用示例

###### 2.1、THREAD_POOL 消费者模式

在`application.yml`文件中增加如下配置

```yaml
canal:
  # 消费者模式；THREAD_POOL, DISRUPTOR
  consumer-mode: THREAD_POOL
  server-mode: TCP
```

创建Java对象 CanalMessageListenerConcurrently，实现消费者监听器 MessageListenerConcurrently 接口

```java
import com.alibaba.fastjson.JSONObject;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.alibaba.otter.canal.spring.boot.consumer.listener.ConsumeConcurrentlyStatus;
import com.alibaba.otter.canal.spring.boot.consumer.listener.MessageListenerConcurrently;
import com.alibaba.otter.canal.spring.boot.utils.CanalUtils;
import com.google.protobuf.ByteString;

import java.util.List;

public class CanalMessageListenerConcurrently  implements MessageListenerConcurrently {

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<Message> messages) throws Exception {
        // 循环所有消息
        for (Message message: messages) {
            // 1、获取 Entry集合
            List<CanalEntry.Entry> entries = message.getEntries();
            long batchId = message.getId();
            CanalUtils.printSummary(message, batchId, entries.size());
            if (batchId == -1 || entries.size() == 0) {
                System.out.println("休息一会吧，当前抓取没有数据");
            } else {
                CanalUtils.printEntry(message.getEntries());
                // 遍历 entryes，单条解析
                for (CanalEntry.Entry entry : entries) {
                    //1，获取表名
                    String tableName = entry.getHeader().getTableName();
                    //2，获取类型
                    CanalEntry.EntryType entryType = entry.getEntryType();
                    //3,获取序列化后的数据
                    ByteString storeValue = entry.getStoreValue();
                    //4,判断当前entryType类型是否为ROWDATA，既当前变化的数据是否行数据
                    if (CanalEntry.EntryType.ROWDATA.equals(entryType)) {
                        //5,反序列化数据
                        CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(storeValue);
                        //6，获取当前事件的操作类型
                        CanalEntry.EventType eventType = rowChange.getEventType();
                        //7,获取数据集
                        List<CanalEntry.RowData> rowDatasList = rowChange.getRowDatasList();
                        //8,遍历RowDataList，并打印数据集
                        for (CanalEntry.RowData rowData : rowDatasList) {
                            JSONObject beforeData = new JSONObject();
                            List<CanalEntry.Column> beforeColumnsList = rowData.getBeforeColumnsList();
                            for (CanalEntry.Column column : beforeColumnsList) {
                                beforeData.put(column.getName(), column.getValue());
                            }
                            JSONObject affterData = new JSONObject();
                            List<CanalEntry.Column> afterColumnsList = rowData.getAfterColumnsList();
                            for (CanalEntry.Column column : afterColumnsList) {
                                affterData.put(column.getName(), column.getValue());
                            }

                            System.out.println("Table:" + tableName +
                                    ",EventType:" + eventType +
                                    ",Before:" + beforeData +
                                    ",After:" + affterData);
                        }

                    } else {
                        System.out.println("当前操作类型为：" + entryType);
                    }
                }
            }
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

}
```

Spring Boot 启动入口：

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CanalApplication_Test {

    @Bean
    public CanalMessageListenerConcurrently canalMessageListener(){
        return new CanalMessageListenerConcurrently();
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(CanalApplication_Test.class, args);
    }

}
```

###### 2.2、DISRUPTOR 消费者模式

在`application.yml`文件中增加如下配置

```yaml
canal:
  # 消费者模式；THREAD_POOL, DISRUPTOR
  consumer-mode: DISRUPTOR
  server-mode: TCP
```

创建Java对象 CanalMessageEventHandler，实现disruptor的事件处理器 MessageEventHandler 接口

```java
import com.alibaba.fastjson.JSONObject;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.alibaba.otter.canal.spring.boot.disruptor.MessageEventHandler;
import com.alibaba.otter.canal.spring.boot.disruptor.event.MessageEvent;
import com.alibaba.otter.canal.spring.boot.utils.CanalUtils;
import com.google.protobuf.ByteString;

import java.util.List;

public class CanalMessageEventHandler implements MessageEventHandler {

    @Override
    public void onEvent(MessageEvent event) throws Exception {
        // 循环所有消息
        for (Message message: event.getMessages()) {
            // 1、获取 Entry集合
            List<CanalEntry.Entry> entries = message.getEntries();
            long batchId = message.getId();
            CanalUtils.printSummary(message, batchId, entries.size());
            if (batchId == -1 || entries.size() == 0) {
                System.out.println("休息一会吧，当前抓取没有数据");
            } else {
                CanalUtils.printEntry(message.getEntries());
                // 遍历 entryes，单条解析
                for (CanalEntry.Entry entry : entries) {
                    //1，获取表名
                    String tableName = entry.getHeader().getTableName();
                    //2，获取类型
                    CanalEntry.EntryType entryType = entry.getEntryType();
                    //3,获取序列化后的数据
                    ByteString storeValue = entry.getStoreValue();
                    //4,判断当前entryType类型是否为ROWDATA，既当前变化的数据是否行数据
                    if (CanalEntry.EntryType.ROWDATA.equals(entryType)) {
                        //5,反序列化数据
                        CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(storeValue);
                        //6，获取当前事件的操作类型
                        CanalEntry.EventType eventType = rowChange.getEventType();
                        //7,获取数据集
                        List<CanalEntry.RowData> rowDatasList = rowChange.getRowDatasList();
                        //8,遍历RowDataList，并打印数据集
                        for (CanalEntry.RowData rowData : rowDatasList) {
                            JSONObject beforeData = new JSONObject();
                            List<CanalEntry.Column> beforeColumnsList = rowData.getBeforeColumnsList();
                            for (CanalEntry.Column column : beforeColumnsList) {
                                beforeData.put(column.getName(), column.getValue());
                            }
                            JSONObject affterData = new JSONObject();
                            List<CanalEntry.Column> afterColumnsList = rowData.getAfterColumnsList();
                            for (CanalEntry.Column column : afterColumnsList) {
                                affterData.put(column.getName(), column.getValue());
                            }

                            System.out.println("Table:" + tableName +
                                    ",EventType:" + eventType +
                                    ",Before:" + beforeData +
                                    ",After:" + affterData);
                        }

                    } else {
                        System.out.println("当前操作类型为：" + entryType);
                    }
                }
            }
        }
    }

}
```

Spring Boot 启动入口：

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CanalDisruptorApplication_Test {

    @Bean
    public CanalMessageEventHandler canalMessageEventHandler(){
        return new CanalMessageEventHandler();
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(CanalDisruptorApplication_Test.class, args);
    }

}
```

###### 2.3、服务端模式

> 默认使用TCP方式连接 Canal Server ，可选择其他方式 ：TCP, KAFKA, ROCKETMQ, RABBITMQ, PULSARMQ




## Jeebiz 技术社区

Jeebiz 技术社区 **微信公共号**、**小程序**，欢迎关注反馈意见和一起交流，关注公众号回复「Jeebiz」拉你入群。

|公共号|小程序|
|---|---|
| ![](https://raw.githubusercontent.com/hiwepy/static/main/images/qrcode_for_gh_1d965ea2dfd1_344.jpg)| ![](https://raw.githubusercontent.com/hiwepy/static/main/images/gh_09d7d00da63e_344.jpg)|