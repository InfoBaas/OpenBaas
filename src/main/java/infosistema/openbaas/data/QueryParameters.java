package infosistema.openbaas.data;

import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.utils.Const;

import org.codehaus.jettison.json.JSONObject;

public class QueryParameters {

	public static final String OPER = "oper";
	public static final String OP1 = "op1";
	public static final String OP2 = "op2";
	public static final String OPER_AND = "and";
	public static final String OPER_OR = "or";
	public static final String OPER_NOT = "not";
	public static final String OPER_CONTAINS = "contains";
	public static final String OPER_EQUALS = "equals";
	public static final String OPER_GREATER = "greater";
	public static final String OPER_LESSER = "lesser";
	public static final String ATTR_PATH = "path";
	public static final String ATTR_ATTRIBUTE = "attribute";
	public static final String ATTR_VALUE = "value";
	
	private String appId = null;
	private JSONObject query = null;
	private Double radius = null;
	private Double latitude = null;
	private Double longitude = null;
	private Integer pageNumber = Const.getPageNumber();
	private Integer pageSize = Const.getPageSize();
	private String orderBy = Const.getOrderBy();
	private String orderType = Const.getOrderType();
	private ModelEnum type = null;

	private QueryParameters() {
	}

	public static QueryParameters getQueryParameters(String appId, JSONObject query, String radiusStr, String latitudeStr, 
			String longitudeStr, String pageNumberStr, String pageSizeStr, String orderByStr, String orderTypeStr, ModelEnum type) {
		QueryParameters retObj = new QueryParameters();

		retObj.setAppId(appId);
		retObj.setQuery(query);
		try {
			retObj.setRadius(Double.parseDouble(radiusStr));
		} catch (Exception e) { }
		try {
			retObj.setLatitude(Double.parseDouble(latitudeStr));
		} catch (Exception e) { }
		try {
			retObj.setLongitude(Double.parseDouble(longitudeStr));
		} catch (Exception e) { }
		try {
			retObj.setPageNumber(Integer.parseInt(pageNumberStr));
		} catch (Exception e) { }
		try {
			retObj.setPageSize(Integer.parseInt(pageSizeStr));
		} catch (Exception e) { }
		retObj.setOrderBy(orderByStr);
		retObj.setOrderType(orderTypeStr);
		retObj.setType(type);
		
		return retObj;

	}

	public String getAppId() {
		return this.appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public JSONObject getQuery() {
		return this.query;
	}

	public void setQuery(JSONObject query) {
		this.query = query;
	}

	public Double getRadius() {
		return this.radius;
	}

	public void setRadius(Double radius) {
		this.radius = radius;
	}

	public Double getLatitude() {
		return this.latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return this.longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Integer getPageNumber() {
		return this.pageNumber;
	}

	public void setPageNumber(Integer pageNumber) {
		this.pageNumber = pageNumber;
	}

	public Integer getPageSize() {
		return this.pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	public String getOrderBy() {
		return this.orderBy;
	}

	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
	}

	public String getOrderType() {
		return this.orderType;
	}

	public void setOrderType(String orderType) {
		this.orderType = orderType;
	}

	public ModelEnum getType() {
		return this.type;
	}

	public void setType(ModelEnum type) {
		this.type = type;
	}
}
