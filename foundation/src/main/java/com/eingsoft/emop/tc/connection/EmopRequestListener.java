package com.eingsoft.emop.tc.connection;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.service.impl.TcBOMServiceImpl;
import com.teamcenter.soa.client.RequestListener;

/**
 * This implementation of the RequestListener, logs each service request to the console.
 *
 */
@Log4j2
public class EmopRequestListener implements RequestListener {

    private final static EmopRequestListener instance = new EmopRequestListener();
    private final static String TC_SERVICE_PACKAGE = TcBOMServiceImpl.class.getPackage().getName();

    private EmopRequestListener() {};

    /**
     * Called before each request is sent to the server.
     */
    @Override
    public void serviceRequest(final Info info) {
        log.info(info.id + ": " + info.service + "." + info.operation+", request:");
        log.info(info.xmlDocument);
    }

    /**
     * Called after each response from the server. Log the service operation to the console.
     */
    @Override
    public void serviceResponse(final Info info) {
        log.info(info.id + ": " + info.service + "." + info.operation+", response:");
        log.info(info.xmlDocument);
    }

    public static EmopRequestListener getInstance() {
        return instance;
    }

    public static class SOADiagnosticInfo {

        @Getter
        private final String username;
        private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        private long requestStartTime = 0;
        private int count = 0;
        private List<Row> data = new ArrayList<Row>();
        // in case there are multi-thread
        private List<SOADiagnosticInfo> children = new ArrayList<EmopRequestListener.SOADiagnosticInfo>();
        private boolean traceSoaUsage;
        private CallTreeNode root = new CallTreeNode("root");

        public SOADiagnosticInfo(String username, boolean traceSoaUsage) {
            this.username = username;
            if(traceSoaUsage) {
              log.info("enabled trace soa usage, it will slow down the performance.");
            }
            this.traceSoaUsage = traceSoaUsage;
        }

        public void startRequest() {
            requestStartTime = System.currentTimeMillis();
        }

        public void endRequest() {
            long soaTime = System.currentTimeMillis() - requestStartTime;
            data.add(new Row(getClosestEMOPSignatureAndSetCallTree(), (int)soaTime));
        }

        public void addChild(SOADiagnosticInfo info) {
            this.children.add(info);
        }

        private String toString(StackTraceElement s) {
          String signature = s.getClassName();
          int pos = signature.lastIndexOf('.');
          if (pos >= 0) {
            signature = signature.substring(pos + 1);
          }
          signature += "." + s.getMethodName() + "(line:" + s.getLineNumber() + ")";
          return signature;
        }
        
        public String getClosestEMOPSignatureAndSetCallTree() {
          StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
          StackTraceElement stackElement = Arrays.stream(stacktrace).filter(s -> {
            return s.getClassName().startsWith(TC_SERVICE_PACKAGE);
          }).findFirst().orElse(null);
          if(stackElement == null) {
              stackElement = Arrays.stream(stacktrace).filter(s -> {
                  // closest tc service
                  return s.getClassName().startsWith("com.eingsoft") && s.getClassName().endsWith("Service");
              }).findFirst().orElse(null);
          }
          String soaMethod = stackElement == null ? "TCSOAServiceUsageNotFound" : toString(stackElement);
          if (traceSoaUsage) {
            List<StackTraceElement> elements = Arrays.stream(stacktrace).filter(s -> {
              return s.getClassName().startsWith("com.eingsoft") && !s.getClassName().startsWith(EmopRequestListener.class.getName())
                  && !s.toString().contains("$");
            }).collect(Collectors.toList());
            CallTreeNode currentNode = root;
            Collections.reverse(elements);
            for(StackTraceElement s : elements) {
              String signature = toString(s);
              CallTreeNode sameNode = currentNode.getChildren().stream().filter(c -> c.getMethodSignature().equals(signature)).findFirst().orElse(null);
              if(sameNode == null) {
                sameNode = new CallTreeNode(signature);
                currentNode.getChildren().add(sameNode);
              }
              sameNode.addSoaMethod(soaMethod);
              currentNode = sameNode;
            }
          }
          return soaMethod;
        }

        public int getRequestIndex() {
            return count++;
        }

        @EqualsAndHashCode(of = {"signature"})
        private static class Row {
            @Getter
            private String signature;
            @Getter
            private int time;
            @Getter
            private Date creationDate = new Date();

            public Row(String signature, int time) {
                this.signature = signature;
                this.time = time;
            }
            
            @Override
            public String toString() {
              return signature + ":" + time;
            }
        }
        
        @Data
        public static class CallTreeNode {
          private final String methodSignature;
          private Map<String, Row> soaMethods = new HashMap<>();
          private List<CallTreeNode> children = new ArrayList<>();
    
          public void addSoaMethod(String soaSignature) {
            Row existing = soaMethods.get(soaSignature);
            if (existing == null) {
              soaMethods.put(soaSignature, new Row(soaSignature, 1));
            } else {
              existing.time = existing.time + 1;
            }
          }
          
          private void print(StringBuilder buffer, String prefix, String childrenPrefix) {
            buffer.append(prefix);
            buffer.append(methodSignature).append("   ");
            buffer.append(soaMethods.values().toString());
            buffer.append('\n');
            for (Iterator<CallTreeNode> it = children.iterator(); it.hasNext();) {
              CallTreeNode next = it.next();
              if (it.hasNext()) {
                next.print(buffer, childrenPrefix + "├── ", childrenPrefix + "│   ");
              } else {
                next.print(buffer, childrenPrefix + "└── ", childrenPrefix + "    ");
              }
            }
          }
          
          @Override
          public String toString() {
            StringBuilder buffer = new StringBuilder(50);
            print(buffer, "", "");
            return buffer.toString();
          }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n==========SOADiagnosticInfo" + this.hashCode() + "==========\n");
            sb.append(username + "@" + sdf.format(new Date()) + " SOA Total Time: "
                + data.stream().mapToInt(d -> d.time).sum() + " ms, " + data.size() + " requests\n");
            Map<String, IntSummaryStatistics> summaryMap =
                data.stream()
                    .collect(Collectors.groupingBy(Row::getSignature, Collectors.summarizingInt(Row::getTime)));
            List<Entry<String, IntSummaryStatistics>> entries = new ArrayList<>(summaryMap.entrySet());
            entries.sort((e1, e2) -> {
                return (int)(e1.getValue().getSum() - e2.getValue().getSum());
            });
            sb.append("Time By Signature: \n" + entries.stream().map(e -> {
                return e.getKey() + " : " + e.getValue().getSum() + " ms (" + e.getValue().getCount() + " count)";
            }).collect(Collectors.joining("\n"))).append("\n");
            sb.append("Detailed Information:\n");
            sb.append(data.stream().map(d -> {
                return d.getSignature() + " : " + sdf.format(d.getCreationDate()) + ", " + d.time + " ms";
            }).collect(Collectors.joining("\n"))).append("\n");
            if(traceSoaUsage) {
              sb.append("Detailed call tree:\n");
              sb.append(root.toString());
            }
            sb.append("==========SOADiagnosticInfo" + this.hashCode() + "==========");
            for (SOADiagnosticInfo child : children) {
                sb.append("SOADiagnosticInfo" + this.hashCode() + "'s Children:\n");
                sb.append(child);
            }
            return sb.toString();
        }

        public boolean hasData() {
            return !data.isEmpty() || !children.isEmpty();
        }
    }
}
