package com.retailpulse;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class ReportMicroserviceTest {

    @Test
    void main_invokesSpringApplicationRun() {
        String[] args = {"--spring.profiles.active=test"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            ReportMicroservice.main(args);

            springApplication.verify(() -> SpringApplication.run(ReportMicroservice.class, args));
        }
    }
}
