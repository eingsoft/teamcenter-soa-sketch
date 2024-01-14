package com.eingsoft.emop.tc.service.impl;

import static com.eingsoft.emop.tc.util.ProxyUtil.spy;

import java.util.List;
import java.util.function.Function;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import com.eingsoft.emop.tc.BMIDE;
import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.service.TcBOMPrintService;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.util.ProxyUtil;
import com.teamcenter.soa.client.model.strong.BOMLine;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.client.model.strong.MEProcessRevision;

@Log4j2
@ScopeDesc(Scope.TcContextHolder)
public class TcBOMPrintServiceImpl implements TcBOMPrintService {

	@Getter
	private final TcContextHolder tcContextHolder;

	public TcBOMPrintServiceImpl(TcContextHolder tcContextHolder) {
		this.tcContextHolder = tcContextHolder;
	}

	@Override
	public String pretty(@NonNull com.teamcenter.soa.client.model.ModelObject topBomline,
			Function<ModelObject, String> toString) {
		return printBOMLine(spy(topBomline, getTcContextHolder()), 1, toString);
	}

	@Override
	public String pretty(@NonNull com.teamcenter.soa.client.model.ModelObject topBomline) {
		return printBOMLine(topBomline, 1,
				(bomline) -> ((ModelObject) bomline.get(BMIDE.PROP_BL_ITEM)).get(BMIDE.PROP_ITEM_ID) + "/"
						+ ((ModelObject) bomline.get(BMIDE.PROP_BL_REVISION)).get(BMIDE.PROP_ITEM_REV_ID));
	}

	private String printBOMLine(com.teamcenter.soa.client.model.ModelObject bomLine, int depth,
			Function<ModelObject, String> toString) {
		StringBuffer sb = new StringBuffer();
		sb.append(toString.apply(spy(bomLine, tcContextHolder))).append("\n");
		List<ModelObject> children = (List<ModelObject>) spy(bomLine, tcContextHolder).get(
				BMIDE.PROP_BL_ALL_CHILD_LINES);
		for (ModelObject child : children) {
			if (child instanceof BOMLine) {
				for (int i = 0; i < depth; i++) {
					sb.append("  ");
				}
				sb.append(printBOMLine(child, depth + 1, toString));
			}
		}
		return sb.toString();
	}

	@Override
	public String pretty(@NonNull String revUid) {
		return pretty(ProxyUtil.spy(getTopLine(revUid), getTcContextHolder()));
	}

	@Override
	public String pretty(String revUid, Function<ModelObject, String> toString) {
		return pretty(ProxyUtil.spy(getTopLine(revUid), getTcContextHolder()), toString);
	}

	private BOMLine getTopLine(String revUid) {
		ItemRevision rev = getTcContextHolder().getTcLoadService().getItemRevision(revUid);
		if (rev instanceof MEProcessRevision) {
			return getTcContextHolder().getTcBOPService().preLoadBOM((MEProcessRevision) rev);
		} else {
			return getTcContextHolder().getTcBOMService().preLoadBOM(rev);
		}
	}
}
