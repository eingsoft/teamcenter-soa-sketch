package com.eingsoft.emop.tc.service;

import java.util.function.Function;

import lombok.NonNull;

import com.teamcenter.soa.client.model.ModelObject;

public interface TcBOMPrintService extends TcService {

	/**
	 * pretty printable format of the whole BOM/BOP window using a customized
	 * {@link Function} to print the information of each bom line.
	 * 
	 * @param topBomline
	 *            the top line of the BOM/BOP tree, it could be either a bom
	 *            line or a bop line
	 * @param toString
	 *            the function to print a bom line
	 * @return a well-formatted string of the whole tree
	 */
	String pretty(@NonNull ModelObject topBomline, Function<com.eingsoft.emop.tc.model.ModelObject, String> toString);

	/**
	 * pretty printable format of the whole BOM/BOP window using a customized
	 * {@link Function} to print the information of each bom line.
	 * 
	 * @param revUid
	 *            item revision uid or process revision uid
	 * @param toString
	 *            the function to print a bom line
	 * @return a well-formatted string of the whole tree
	 */
	String pretty(@NonNull String revUid, Function<com.eingsoft.emop.tc.model.ModelObject, String> toString);

	/**
	 * pretty printable format of the whole BOM/BOP window using default
	 * function
	 * 
	 * @param topBomline
	 *            the top line of the BOM/BOP tree, it could be either a bom
	 *            line or a bop line
	 * @return a well-formatted string of the whole tree
	 */
	String pretty(ModelObject topBomline);

	/**
	 * pretty printable format of the whole BOM/BOP window using default
	 * function
	 * 
	 * @param revUid
	 *            item revision uid or process revision uid
	 * @return a well-formatted string of the whole tree
	 */
	String pretty(String revUid);

}
