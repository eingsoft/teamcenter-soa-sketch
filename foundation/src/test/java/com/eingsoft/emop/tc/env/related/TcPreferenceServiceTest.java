package com.eingsoft.emop.tc.env.related;

import java.util.List;

import org.junit.*;

import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.service.CredentialTcContextHolder;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcPreferenceService;

import lombok.extern.log4j.Log4j2;

@Ignore
@Log4j2
public class TcPreferenceServiceTest {
    @Before
    public void setup() {
        System.setProperty("tc.protocol", "http");
        System.setProperty("tc.host", "120.78.87.76");
        System.setProperty("tc.port", "7001");
        System.setProperty("tc.appName", "tc");
        SOAExecutionContext.current().initWithoutPool("zhicheng", "zhicheng");
    }
    
    @After
    public void teardown() {
        SOAExecutionContext.current().cleanupSiliently();
    }

    @Test
    public void test1() {
        TcPreferenceService tcPrefSvc = SOAExecutionContext.current().getTcContextHolder().getTcPreferenceService();
        
        String val1 = tcPrefSvc.getStringValue("OP4M_REGISTER");
        log.debug("OP4M_REGISTER = " + val1);
        Assert.assertTrue(val1 != null && !val1.isEmpty());
        
        List<String> val2 = tcPrefSvc.getStringValues("OP4M_REGISTER");
        log.debug("OP4M_REGISTER = " + val2);
        Assert.assertTrue(val2.size() > 0);

        String val3 = tcPrefSvc.getStringValue("CAE_tool_release_status");
        log.debug("CAE_tool_release_status = " + val3);
        Assert.assertTrue(val3 != null && !val3.isEmpty());

        List<String> val4 = tcPrefSvc.getStringValues("CAE_tool_release_status");
        log.debug("CAE_tool_release_status = " + val4);
        Assert.assertTrue(val4.size() == 1);
        
        String val5 = tcPrefSvc.getStringValue("BYPASS_RULES");
        log.debug("BYPASS_RULES = " + val5);
        Assert.assertTrue(val5 != null && !val5.isEmpty());

        List<String> val6 = tcPrefSvc.getStringValues("BYPASS_RULES");
        log.debug("BYPASS_RULES = " + val6);
        Assert.assertTrue(val6.size() == 1);
        
        String val7 = tcPrefSvc.getStringValue("NOT_EXISTS_PREF_VALUE");
        log.debug("NOT_EXISTS_PREF_VALUE = " + val7);
        Assert.assertTrue(val7 == null);

        List<String> val8 = tcPrefSvc.getStringValues("NOT_EXISTS_PREF_VALUE");
        log.debug("NOT_EXISTS_PREF_VALUE = " + val8);
        Assert.assertTrue(val8.size() == 0);
    }
}
