package com.portfolio.saga;

import com.portfolio.saga.benchmark.BenchmarkRunner;
import com.portfolio.saga.benchmark.ConsistencyResult;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SagaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SagaApplication.class, args);
    }

    @Bean
    public CommandLineRunner benchmarkRunner() {
        return args -> {
            boolean runBenchmark = args.length == 0 || "--benchmark".equals(args[0]);
            if (runBenchmark) {
                BenchmarkRunner runner = new BenchmarkRunner(100, 0.2, 42);
                ConsistencyResult result = runner.run();
                System.out.println("=== BENCHMARK RESULT ===");
                System.out.println(result.toJson());
                System.out.println("=== END ===");
                System.exit(0);
            }
        };
    }
}
