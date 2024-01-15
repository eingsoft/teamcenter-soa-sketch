package com.eingsoft.emop.tc.env.related;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.service.CredentialTcContextHolder;
import com.eingsoft.emop.tc.service.TcClassificationService;
import com.eingsoft.emop.tc.service.TcContextHolder;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Ignore
public class TcClassificationServiceTest {
    @Before
    public void setup() {
        System.setProperty("tc.protocol", "http");
        System.setProperty("tc.host", "192.168.1.24");
        System.setProperty("tc.port", "7001");
        System.setProperty("tc.appName", "tc");
        SOAExecutionContext.current().initWithoutPool("test004", "1");
    }

    @After
    public void teardown() {
        SOAExecutionContext.current().cleanupSiliently();
    }

    @Test
    public void testSeachHearderPath() {
        TcClassificationService classificationService =
            SOAExecutionContext.current().getTcContextHolder().getTcClassificationService();
        log.info("D*04-CIDPath：" + classificationService.searchHearderPath("D*04", false));
        log.info("NamePath1：" + classificationService.searchHearderPath("淋浴器*", true));
        log.info("NamePath2：" + classificationService.searchHearderPath("马桶", true));
    }

    @Test
    public void testSearchParentsCID() {
        TcClassificationService classificationService =
            SOAExecutionContext.current().getTcContextHolder().getTcClassificationService();
        log.info("Root-Parent：" + classificationService.searchParentsByCID("SAM"));
        log.info("D104-Parent：" + classificationService.searchParentsByCID("D104"));
    }

    @Test
    public void testGetClassAttribute(){
        TcClassificationService classificationService = SOAExecutionContext.current().getTcContextHolder().getTcClassificationService();
        classificationService.getClassificationAttributes("-150000");
    }
}
