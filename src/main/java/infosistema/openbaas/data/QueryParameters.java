package infosistema.openbaas.data;

import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.utils.Const;

import org.codehaus.jettison.json.JSONObject;

public class QueryParameters {

	public static final String ATTR_ATTRIBUTE = "attribute";
	public static final String ATTR_VALUE = "value";

	private String appId = null;
	private String userId = null;
	private JSONObject query = null;
	private Double radius = null;
	private Double latitude = null;
	private Double longitude = null;
	private Integer pageNumber = Const.getPageNumber();
	private Integer pageSize = Const.getPageSize();
	private String orderBy = Const.getOrderBy();
	private String orderType = Const.getOrderType();
	private String url = "";
	
	private ModelEnum type = null;

	private QueryParameters() {
	}

	public static QueryParameters getQueryParameters(String appId, String userId, JSONObject query, String radiusStr, String latitudeStr, 
			String longitudeStr, String pageNumberStr, String pageSizeStr, String orderByStr, String orderTypeStr, ModelEnum type) {
		return getQueryParameters(appId, userId, query, radiusStr, latitudeStr, longitudeStr, pageNumberStr, pageSizeStr,
				orderByStr, orderTypeStr, null, type); 
	}

	public static QueryParameters getQueryParameters(String appId, String userId, JSONObject query, String radiusStr, String latitudeStr, 
			String longitudeStr, String pageNumberStr, String pageSizeStr, String orderByStr, String orderTypeStr, String url, ModelEnum type) {
		QueryParameters retObj = new QueryParameters();

		retObj.setAppId(appId);
		retObj.setUserId(userId);
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
		if(orderByStr!=null)
			retObj.setOrderBy(orderByStr);
		else
			retObj.setOrderBy(Const.DEFAULT_ORDER_BY);
		if(orderTypeStr!=null)
			retObj.setOrderType(orderTypeStr);
		else
			retObj.setOrderType(Const.DEFAULT_ORDER_TYPE);
		retObj.setUrl(url);
		retObj.setType(type);
		
		return retObj;

	}

	public String getAppId() {
		return this.appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getUserId() {
		return this.userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
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

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public ModelEnum getType() {
		return this.type;
	}

	public void setType(ModelEnum type) {
		this.type = type;
	}
}
