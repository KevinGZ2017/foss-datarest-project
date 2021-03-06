/*******************************************************************************
 * Copyright (c) Oct 8, 2016 @author <a href="mailto:iffiff1@gmail.com">Tyler Chen</a>.
 * All rights reserved.
 *
 * Contributors:
 *     <a href="mailto:iffiff1@gmail.com">Tyler Chen</a> - initial API and implementation
 ******************************************************************************/
package com.foreveross.datarest.core.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:iffiff1@gmail.com">Tyler Chen</a> 
 * @since Oct 8, 2016
 */
@SuppressWarnings("serial")
public class DescTableModel implements Serializable {

	private String catalog;
	private String schema;
	private String name;
	private String type;
	private String remarks;
	private List<DescColumnModel> columns = new ArrayList<DescColumnModel>();

	public DescTableModel() {
		super();
	}

	public static DescTableModel create(String catalog, String schema, String name, String type, String remarks) {
		DescTableModel descTable = new DescTableModel();
		descTable.catalog = catalog;
		descTable.schema = schema;
		descTable.name = name;
		descTable.type = type;
		descTable.remarks = remarks;
		return descTable;
	}

	public DescTableModel add(DescColumnModel column) {
		columns.add(column);
		return this;
	}

	public String getCatalog() {
		return catalog;
	}

	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	public List<DescColumnModel> getColumns() {
		return columns;
	}

	public void setColumns(List<DescColumnModel> columns) {
		this.columns = columns;
	}

	@Override
	public String toString() {
		return "DescTable [catalog=" + catalog + ", schema=" + schema + ", name=" + name + ", type=" + type
				+ ", remarks=" + remarks + ", columns=" + columns + "]";
	}
}
