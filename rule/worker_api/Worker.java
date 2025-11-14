package top.fifthlight.bazel.worker.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Worker {
    public void run(String... args) throws Exception {
        var isPersistentWorker = Arrays.asList(args).contains("--persistent_worker");

        try (var executor = Executors.newFixedThreadPool(4)) {
            var mapper = new ObjectMapper().configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
            var reader = new BufferedReader(new InputStreamReader(System.in));
            var requests = new LinkedList<CompletableFuture<Void>>();
            var outputLock = new ReentrantLock();
            while (true) {
                var line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.isEmpty()) {
                    continue;
                }
                var request = mapper.readValue(line, WorkRequest.class);
                if (request.requestId() != 0) {
                    requests.add(CompletableFuture.runAsync(() -> {
                        var output = new StringWriter();
                        try {
                            var exitCode = handleRequest(request, new PrintWriter(output));
                            var response = new WorkResponse(exitCode, output.toString(), request.requestId());
                            outputLock.lock();
                            mapper.writeValue(System.out, response);
                            System.out.println();
                        } catch (Exception e) {
                            e.printStackTrace(new PrintWriter(output));
                            var response = new WorkResponse(1, output.toString(), 0);
                            outputLock.lock();
                            try {
                                mapper.writeValue(System.out, response);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        } finally {
                            outputLock.unlock();
                        }
                    }, executor));
                } else {
                    for (var future : requests) {
                        future.join();
                    }
                    outputLock.lock();
                    var output = new StringWriter();
                    try {
                        var exitCode = handleRequest(request, new PrintWriter(output));
                        var response = new WorkResponse(exitCode, output.toString(), 0);
                        mapper.writeValue(System.out, response);
                        System.out.println();
                    } catch (Exception e) {
                        e.printStackTrace(new PrintWriter(output));
                        var response = new WorkResponse(1, output.toString(), 0);
                        mapper.writeValue(System.out, response);
                    } finally {
                        outputLock.unlock();
                        System.out.flush();
                    }
                }
                if (!isPersistentWorker) {
                    break;
                }
            }
            System.gc();
        }
    }

    protected abstract int handleRequest(WorkRequest request, PrintWriter out);
}
