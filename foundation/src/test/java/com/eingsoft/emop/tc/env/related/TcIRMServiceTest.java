package com.eingsoft.emop.tc.env.related;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.service.CredentialTcContextHolder;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcIRMService;
import com.eingsoft.emop.tc.service.TcPreferenceService;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Ignore
public class TcIRMServiceTest {
    @Before
    public void setup() {
        System.setProperty("tc.protocol", "http");
        System.setProperty("tc.host", "120.78.87.76");
        System.setProperty("tc.port", "7001");
        System.setProperty("tc.appName", "tc");
        SOAExecutionContext.current().init("zhicheng", "zhicheng");
    }

    @After
    public void teardown() {
        SOAExecutionContext.current().cleanupSiliently();
    }

    @Test
    public void testPerference() {
        TcPreferenceService tcPrefSvc = SOAExecutionContext.current().getTcContextHolder().getTcPreferenceService();

        String val1 = tcPrefSvc.getStringValue("JM8_CBBCompoundRuleConfigure");
        log.debug("OP4M_REGISTER = " + val1);
//        Assert.assertTrue(val1 != null && !val1.isEmpty());

        List<String> val2 = tcPrefSvc.getStringValues("JM8_OP4M_REGISTER");
        log.debug("OP4M_REGISTER = " + val2);
//        Assert.assertTrue(val2.size() > 0);
    }

    @Test
    public void testWritePrivilege() {
        TcIRMService irmService = SOAExecutionContext.current().getTcContextHolder().getTcIRMService();
        boolean value = irmService.hasWritePrivilege("gaXtpbnJJMlDLB");
        log.info("Has Write Privilege:" + value);
    }
}
