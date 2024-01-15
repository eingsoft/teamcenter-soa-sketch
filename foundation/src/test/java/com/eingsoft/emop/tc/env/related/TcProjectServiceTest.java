package com.eingsoft.emop.tc.env.related;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.eingsoft.emop.tc.BMIDE;
import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.service.CredentialTcContextHolder;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.util.ProxyUtil;
import com.google.common.collect.Lists;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.exceptions.NotLoadedException;

@Ignore
public class TcProjectServiceTest {

    @Before
    public void init() {}

    private void initContext() {
        System.setProperty("tc.protocol", "http");
        System.setProperty("tc.host", "172.17.95.160");
        System.setProperty("tc.port", "7001");
        System.setProperty("tc.appName", "tc");
        SOAExecutionContext.current().initWithoutPool("king", "king");
    }

    @After
    public void teardown() {
        SOAExecutionContext.current().cleanupSiliently();
    }

    @Test
    public void testAssignObjectsToProjects() throws NotLoadedException {
        initContext();
        TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();
        ModelObject project = contextHolder.getTcLoadService().loadObject("A$Y1BzBVJMlDLB");
        ModelObject revision = contextHolder.getTcLoadService().loadObject("Bmdx47mlJMlDLB");
        System.out.println(ProxyUtil.spy(project, contextHolder).getDisplayVal(BMIDE.PROP_OBJECT_STRING));
        System.out.println(ProxyUtil.spy(revision, contextHolder).getDisplayVal(BMIDE.PROP_OBJECT_STRING));

        if (project == null || revision == null) {
            return;
        }

        boolean remove = contextHolder.getTcProjectService().removeObjects(Lists.newArrayList(revision),
            Lists.newArrayList(project));
        System.out.println("remove:" + remove);

        boolean assign = contextHolder.getTcProjectService().assignObjects(Lists.newArrayList(revision),
            Lists.newArrayList(project));
        System.out.println("assign:" + assign);

    }

}
