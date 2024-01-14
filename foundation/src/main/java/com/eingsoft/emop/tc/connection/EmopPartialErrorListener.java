package com.eingsoft.emop.tc.connection;

import com.eingsoft.emop.tc.BMIDE;
import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.service.TcContextHolderAware;
import com.eingsoft.emop.tc.util.ProxyUtil;
import com.teamcenter.soa.client.model.ErrorStack;
import com.teamcenter.soa.client.model.ErrorValue;
import com.teamcenter.soa.client.model.PartialErrorListener;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class EmopPartialErrorListener implements PartialErrorListener, TcContextHolderAware {

  @Override
  public void handlePartialError(ErrorStack[] stacks) {
    if (stacks.length == 0) {
      return;
    }

    StringBuffer sb = new StringBuffer(1000);
    sb.append("***************************\n");
    sb.append("Partial Errors caught:\n");

    for (int i = 0; i < stacks.length; i++) {
      ErrorValue[] errors = stacks[i].getErrorValues();
      // The different service implementation may optionally associate
      // an ModelObject, client ID, or nothing, with each partial error
      if (stacks[i].hasAssociatedObject()) {
        ModelObject spyObj = ProxyUtil.spy(stacks[i].getAssociatedObject(), getTcContextHolder());
        sb.append("object " + spyObj.getDisplayVal(BMIDE.PROP_OBJECT_STRING) + " [" + spyObj.getUid() + "]\n");
      } else if (stacks[i].hasClientId()) {
        sb.append("client id " + stacks[i].getClientId() + "\n");
      } else if (stacks[i].hasClientIndex())
        sb.append("client index " + stacks[i].getClientIndex() + "\n");

      // Each Partial Error will have one or more contributing error messages
      for (int j = 0; j < errors.length; j++) {
        sb.append("    Code: " + errors[j].getCode() + "\tSeverity: " + errors[j].getLevel() + "\t" + errors[j].getMessage() + "\n");
      }
    }
    sb.append("***************************");
    SOAExecutionContext.current().getPartialErrors().add(sb.toString());
    log.warn(sb.toString());
  }
}
