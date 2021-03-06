/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zeppelin.spark;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.spark.SparkContext;
import org.apache.spark.sql.SQLContext;
import org.apache.zeppelin.interpreter.Interpreter;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterGroup;
import org.apache.zeppelin.interpreter.InterpreterException;
import org.apache.zeppelin.interpreter.InterpreterPropertyBuilder;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResult.Code;
import org.apache.zeppelin.interpreter.LazyOpenInterpreter;
import org.apache.zeppelin.interpreter.WrappedInterpreter;
import org.apache.zeppelin.scheduler.Scheduler;
import org.apache.zeppelin.scheduler.SchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spark SQL interpreter for Zeppelin.
 *
 * @author Leemoonsoo
 *
 */
public class SparkSqlInterpreter extends Interpreter {
  Logger logger = LoggerFactory.getLogger(SparkSqlInterpreter.class);
  AtomicInteger num = new AtomicInteger(0);

  static {
    Interpreter.register(
        "sql",
        "spark",
        SparkSqlInterpreter.class.getName(),
        new InterpreterPropertyBuilder()
            .add("zeppelin.spark.maxResult",
                SparkInterpreter.getSystemDefault("ZEPPELIN_SPARK_MAXRESULT",
                    "zeppelin.spark.maxResult", "1000"),
                "Max number of SparkSQL result to display.")
            .add("zeppelin.spark.concurrentSQL",
                SparkInterpreter.getSystemDefault("ZEPPELIN_SPARK_CONCURRENTSQL",
                    "zeppelin.spark.concurrentSQL", "false"),
                "Execute multiple SQL concurrently if set true.")
            .build());
  }

  private String getJobGroup(InterpreterContext context){
    return "zeppelin-" + context.getParagraphId();
  }

  private int maxResult;

  public SparkSqlInterpreter(Properties property) {
    super(property);
  }

  @Override
  public void open() {
    this.maxResult = Integer.parseInt(getProperty("zeppelin.spark.maxResult"));
  }

  private SparkInterpreter getSparkInterpreter() {
    InterpreterGroup intpGroup = getInterpreterGroup();
    LazyOpenInterpreter lazy = null;
    SparkInterpreter spark = null;
    synchronized (intpGroup) {
      for (Interpreter intp : getInterpreterGroup()){
        if (intp.getClassName().equals(SparkInterpreter.class.getName())) {
          Interpreter p = intp;
          while (p instanceof WrappedInterpreter) {
            if (p instanceof LazyOpenInterpreter) {
              lazy = (LazyOpenInterpreter) p;
            }
            p = ((WrappedInterpreter) p).getInnerInterpreter();
          }
          spark = (SparkInterpreter) p;
        }
      }
    }
    if (lazy != null) {
      lazy.open();
    }
    return spark;
  }

  public boolean concurrentSQL() {
    return Boolean.parseBoolean(getProperty("zeppelin.spark.concurrentSQL"));
  }

  @Override
  public void close() {}

  @Override
  public InterpreterResult interpret(String st, InterpreterContext context) {
    SQLContext sqlc = null;

    sqlc = getSparkInterpreter().getSQLContext();

    SparkContext sc = sqlc.sparkContext();
    if (concurrentSQL()) {
      sc.setLocalProperty("spark.scheduler.pool", "fair");
    } else {
      sc.setLocalProperty("spark.scheduler.pool", null);
    }

    Object rdd = null;
    try {
      // method signature of sqlc.sql() is changed
      // from  def sql(sqlText: String): SchemaRDD (1.2 and prior)
      // to    def sql(sqlText: String): DataFrame (1.3 and later).
      // Therefore need to use reflection to keep binary compatibility for all spark versions.
      Method sqlMethod = sqlc.getClass().getMethod("sql", String.class);
      rdd = sqlMethod.invoke(sqlc, st);
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException e) {
      throw new InterpreterException(e);
    }

    String msg = ZeppelinContext.showDF(sc, context, rdd, maxResult);
    return new InterpreterResult(Code.SUCCESS, msg);
  }

  @Override
  public void cancel(InterpreterContext context) {
    SQLContext sqlc = getSparkInterpreter().getSQLContext();
    SparkContext sc = sqlc.sparkContext();

    sc.cancelJobGroup(getJobGroup(context));
  }

  @Override
  public FormType getFormType() {
    return FormType.SIMPLE;
  }


  @Override
  public int getProgress(InterpreterContext context) {
    SparkInterpreter sparkInterpreter = getSparkInterpreter();
    return sparkInterpreter.getProgress(context);
  }

  @Override
  public Scheduler getScheduler() {
    if (concurrentSQL()) {
      int maxConcurrency = 10;
      return SchedulerFactory.singleton().createOrGetParallelScheduler(
          SparkSqlInterpreter.class.getName() + this.hashCode(), maxConcurrency);
    } else {
      // getSparkInterpreter() calls open() inside.
      // That means if SparkInterpreter is not opened, it'll wait until SparkInterpreter open.
      // In this moment UI displays 'READY' or 'FINISHED' instead of 'PENDING' or 'RUNNING'.
      // It's because of scheduler is not created yet, and scheduler is created by this function.
      // Therefore, we can still use getSparkInterpreter() here, but it's better and safe
      // to getSparkInterpreter without opening it.
      for (Interpreter intp : getInterpreterGroup()) {
        if (intp.getClassName().equals(SparkInterpreter.class.getName())) {
          Interpreter p = intp;
          return p.getScheduler();
        } else {
          continue;
        }
      }
      throw new InterpreterException("Can't find SparkInterpreter");
    }
  }

  @Override
  public List<String> completion(String buf, int cursor) {
    return null;
  }
}
