package com.sellerNet.backManagement.controller.manager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.alipay.util.AliPayMsg;
import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.util.Auth;
import com.sellerNet.backManagement.config.PushConst;
import com.sellerNet.backManagement.controller.BaseController;
import com.sellerNet.backManagement.dao.BaseDao;
import com.sellerNet.backManagement.dao.ImgsItemMapper;
import com.sellerNet.backManagement.dao.NewsMapper;
import com.sellerNet.backManagement.dao.RechargeRecordMapper;
import com.sellerNet.backManagement.dto.CountParm;
import com.sellerNet.backManagement.dto.ImageDto;
import com.sellerNet.backManagement.dto.NewsDto;
import com.sellerNet.backManagement.entity.AgentUser;
import com.sellerNet.backManagement.entity.AppMessage;
import com.sellerNet.backManagement.entity.Evaluate;
import com.sellerNet.backManagement.entity.Image;
import com.sellerNet.backManagement.entity.ImageCategory;
import com.sellerNet.backManagement.entity.ImgsItem;
import com.sellerNet.backManagement.entity.Islike;
import com.sellerNet.backManagement.entity.JsonResult;
import com.sellerNet.backManagement.entity.News;
import com.sellerNet.backManagement.entity.NewsIndex;
import com.sellerNet.backManagement.entity.NewsType;
import com.sellerNet.backManagement.entity.PageEntity;
import com.sellerNet.backManagement.entity.PagingResult;
import com.sellerNet.backManagement.entity.RechargeRecord;
import com.sellerNet.backManagement.entity.Set;
import com.sellerNet.backManagement.entity.User;
import com.sellerNet.backManagement.entity.UserOne;
import com.sellerNet.backManagement.service.AppMessageService;
import com.sellerNet.backManagement.service.AppUserOneService;
import com.sellerNet.backManagement.service.AppUserService;
import com.sellerNet.backManagement.service.EvaluateService;
import com.sellerNet.backManagement.service.ImageService;
import com.sellerNet.backManagement.service.IslikeService;
import com.sellerNet.backManagement.service.ManagerUserService;
import com.sellerNet.backManagement.service.NewsService;
import com.sellerNet.backManagement.service.RoleService;
import com.sellerNet.backManagement.service.SetService;
import com.sellerNet.backManagement.utils.JsonUtils;
import com.sellerNet.backManagement.utils.Md5Utils;
import com.sellerNet.backManagement.utils.StringUtil;

/**
 *资讯
 */
@Controller
@RequestMapping("/admin")
public class NewsController extends BaseController{
	
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(NewsController.class);
	
	@Resource
	private ManagerUserService managerUserService;
	
	@Resource
	private RoleService roleService;
	
	@Resource
	private AppUserOneService appUserOneService;
	
	@Resource
	private AppUserService	 appUserService;
	
	@Resource
	private NewsService newsService;
	@Resource
	private NewsMapper newsMapper;
	@Resource
	private ImageService imageService;
	@Resource
	private SetService setService;
	@Resource
	private RechargeRecordMapper rechargeRecordMapper;
	@Resource
	private IslikeService islikeService;
	@Resource
	private EvaluateService evaluateService;
	@Resource
	private ImgsItemMapper imgsItemMapper;
	@Resource
	private AppMessageService appMessageService;
	
	/**
	 * App端今日新闻管理
	 * @return
	 */
	@RequestMapping(value="/newsManage.do", method = RequestMethod.GET)
	public String newsPage(){
		return "newsManage";
	}
	/**
	 *资讯统计页面 
	 */
	@RequestMapping(value="/newsCount.do", method = RequestMethod.GET)
	public String newsCount(){
		return "newsCount";
	}
	/**
	 *资讯按月统计页面 
	 */
	@RequestMapping(value="/newsCountMonth.do", method = RequestMethod.GET)
	public String newsCountMonth(){
		return "newsCountMonth";
	}
	/**
	 *资讯发布费统计页面 
	 */
	@RequestMapping(value="/newsCountMoney.do", method = RequestMethod.GET)
	public String newsCountMoney(){
		return "newsCountMoney";
	}
	/**
	 *资讯发布费按月统计页面 
	 */
	@RequestMapping(value="/newsCountMonthMoney.do", method = RequestMethod.GET)
	public String newsCountMonthMoney(){
		return "newsCountMonthMoney";
	}
	/**
	 *资讯发布费设置页面 
	 */
	@RequestMapping(value="/newsMoneyConfigManage.do", method = RequestMethod.GET)
	public String newsMoneyConfigManage(){
		return "newsMoneyConfigManage";
	}
	private Integer getNOTNULL(Integer day) {
		return day==null?0:day;
	}
	private Double getNOTNULL(Double dayCountMoney) {
		return dayCountMoney==null?0:dayCountMoney;
	}
	/**
	 * 资讯发布费统计数据
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="/newsCountMoneyData.do", method = RequestMethod.POST)
	public Map<String, Object> newsCountMoneyData(){
		CountParm count =new CountParm();
		Double day =getNOTNULL(newsService.dayCountMoney(null));
		Double month =getNOTNULL(newsService.monthCountMoney(null));
		Double all =getNOTNULL(newsService.allCountMoney(null));
		List<CountParm> list =new ArrayList<CountParm>();
		count.setAll(all.toString());
		count.setMonth(month.toString());
		count.setToday(day.toString());
		list.add(count);
		Map result = new HashMap();
		result.put("rows", list);
		result.put("total", 1);
		return result;
	}
	/**
	 * 资讯发布费按月统计数据
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="/newsCountMonthMoneyData.do", method = RequestMethod.POST)
	public List<Map<String, Object>> newsCountMonthMoneyData(){
		List<Map<String, Object>> result=newsService.countByMonthMoney(null);
		return result;
	}
	/**
	 * 资讯统计数据
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="/newsCountData.do", method = RequestMethod.POST)
	public Map<String, Object> newsCountData(){
		CountParm count =new CountParm();
		Integer day =getNOTNULL(newsService.dayCount(null));
		Integer month =getNOTNULL(newsService.monthCount(null));
		Integer all =getNOTNULL(newsService.allCount(null));
		List<CountParm> list =new ArrayList<CountParm>();
		count.setAll(all.toString());
		count.setMonth(month.toString());
		count.setToday(day.toString());
		list.add(count);
		Map result = new HashMap();
		result.put("rows", list);
		result.put("total", 1);
		return result;
	}
	/**
	 * 资讯按月统计数据
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="/newsCountMonthData.do", method = RequestMethod.POST)
	public List<Map<String, Object>> newsCountMonthData(){
		List<Map<String, Object>> result=newsService.countByMonth(null);
		return result;
	}
	/**
	 * 客户今日新闻管理
	 * @return
	 */
	@RequestMapping(value="/agentNewsManage.do", method = RequestMethod.GET)
	public String agentNewsManage(){
		return "agentNewsManage";
	}
	
	@RequestMapping(value="/html.do")
	public void html(HttpServletResponse response,Long id) throws IOException{
		News news = newsService.get(id);
		response.setContentType("text/html;charset=utf-8");
		response.getWriter().write(news.getContent());
	}
	/**
	 * 获取资讯详情
	 * @return
	 */
	@RequestMapping(value="/getNewsDetail.do")
	public String getNewsDetail(@RequestParam(value = "id",required=false) String id){
		return "newsManage";
	}
	
	/**
	 * 发布资讯
	 */
	@ResponseBody
	@RequestMapping(value="/addNews.do")
	public JsonResult addNews(@RequestParam(value = "title",required=false) String title,
			@RequestParam(value = "titleImages",required=false) String titleImages,
			@RequestParam(value = "type",required=false) String type,
			@RequestParam(value = "content",required=false) String content,
			@RequestParam(value = "pcUserId",required=false) String pcUserId,
			@RequestParam(value = "imgOrVedio",required=false) String imgOrVedio,
			@RequestParam(value = "playLength",required=false) String playLength
			){
		JsonResult jsonResult = new JsonResult<>();
		News news = new News();
		if(StringUtils.isBlank(content)){
			jsonResult.setCode("1");
			jsonResult.setErrorDescription("资讯内容不能为空");
			return jsonResult;
		}
		if(StringUtils.isBlank(titleImages)){
			if(playLength==null){
				jsonResult.setCode("1");
				jsonResult.setErrorDescription("视频播放时长不能为空");
				return jsonResult;
			}
			//视频资讯限制必须且只能上传一个视频
			if((!content.startsWith(""))||(!content.endsWith(""))){
				jsonResult.setCode("1");
				jsonResult.setErrorDescription("视频资讯内容不能上传除视频外的其他内容,能且只能上传一个视频");
				return jsonResult;
			}
			if(content.split("<embed").length>2){
				jsonResult.setCode("1");
				jsonResult.setErrorDescription("视频资讯内容能且只能上传一个视频,请删除多余的视频再上传");
				return jsonResult;
			}
		}
		AgentUser agentUser = managerUserService.get(Long.valueOf(pcUserId));
		//计算该条资讯需要多少钱
		double money=0.00;
		long count = agentUser.getNewsNum();
		Set set = setService.get(4L);
		String newsMoneyConfig = set.getContent();
		String[] configs = newsMoneyConfig.split(",");
		if(count<Double.valueOf(configs[0])){
			money=Double.valueOf(configs[1]);
		}else{
			int max=(configs.length-2)/3;
			int j=2;
			for(int i=0;i<max;i++){
				if(count>=Double.valueOf(configs[j++])&&count<Double.valueOf(configs[j++])){
					money=Double.valueOf(configs[j++]);
					break;
				}
			}
		}
		//判断余额是否充足
		Long userId = agentUser.getUserId();
		UserOne userOne = appUserOneService.get(userId.intValue());
		BigDecimal balance =userOne.getBalance();
		BigDecimal money2 = new BigDecimal(money);
		if(balance.compareTo(money2)==-1){
			jsonResult.setCode("1");
			jsonResult.setErrorDescription("余额不足,发布该资讯需要付费:"+money+"元,请在app充值后再发布");
			return jsonResult;
		}
		//扣钱
		userOne.setBalance(userOne.getBalance().subtract(new BigDecimal(money)));
		appUserOneService.update(userOne);
		news.setContent(content);
		news.setCreatedTime(new Date());
		news.setTitle(title);
		news.setType(type);
		news.setPcUserId(pcUserId);
		news.setImgOrVedio(imgOrVedio);
		news.setUserId(userId);
		news.setPlayLength(playLength);
		try {
			//添加资讯记录统计数量
			newsService.insertRecord(news);
			news.setId(null);
			newsService.insert(news);
			if(StringUtils.isNotBlank(titleImages)){
				//保存标题图片
				Image image = new Image();
				image.setCategory(ImageCategory.TITLEIMAGE.name());
				image.setObjectId(news.getId());
				image.setLastModifier(Long.valueOf(pcUserId));
				image.setLastModifiedTime(new Date());
				image.setCreatedTime(new Date());
				image.setCreator(Long.valueOf(pcUserId));
				image.setUrl(titleImages);
				imageService.insert(image);
			}
			//修改发布资讯次数
			agentUser.setNewsNum((agentUser.getNewsNum()+1));
			managerUserService.update(agentUser);
			//添加资讯收入记录
			Map newsMoneyMap=new HashMap<>();
			newsMoneyMap.put("newsId", news.getId());
			newsMoneyMap.put("money", money);
			newsMoneyMap.put("createdTime", new Date());
			newsService.insertNewsMoney(newsMoneyMap);
			//添加资金明细记录
			RechargeRecord rechargeRecord = new RechargeRecord();
			rechargeRecord.setActive("2");
			rechargeRecord.setAmount(Double.toString(money));
			rechargeRecord.setCreatedTime(new Date());
			rechargeRecord.setCreator(userId);
			rechargeRecord.setLastModifiedTime(new Date());
			rechargeRecord.setLastModifier(userId);
			rechargeRecord.setOrderId(news.getId().toString());
			rechargeRecord.setUserId(userId.intValue());
			rechargeRecordMapper.insert(rechargeRecord);
		} catch (Exception e) {
			LOGGER.error("error", e);
			e.printStackTrace();
			jsonResult.setCode("1");
			jsonResult.setErrorDescription("发布资讯失败");
			return jsonResult;
		}
		jsonResult.setCode("0");
		jsonResult.setErrorDescription("发布资讯成功");
		return jsonResult;
	}
	/**
	 * 修改资讯
	 */
	@ResponseBody
	@RequestMapping(value="/updateNews.do")
	public JsonResult updateNews(@RequestParam(value = "title",required=false) String title,
			@RequestParam(value = "titleImages",required=false) String titleImages,
			@RequestParam(value = "type",required=false) String type,
			@RequestParam(value = "content",required=false) String content,
			@RequestParam(value = "pcUserId",required=false) Long pcUserId,
			@RequestParam(value = "id",required=false) Long id,
			@RequestParam(value = "playLength",required=false) String playLength
			){
		JsonResult jsonResult = new JsonResult<>();
		News news = newsService.get(id);
		if(StringUtils.isBlank(content)){
			jsonResult.setCode("1");
			jsonResult.setErrorDescription("资讯内容不能为空");
			return jsonResult;
		}
		if(StringUtils.isBlank(titleImages)){
			if(playLength==null){
				jsonResult.setCode("1");
				jsonResult.setErrorDescription("视频播放时长不能为空");
				return jsonResult;
			}
			//视频资讯限制必须且只能上传一个视频
			if((!content.startsWith(""))||(!content.endsWith(""))){
				jsonResult.setCode("1");
				jsonResult.setErrorDescription("视频资讯内容不能上传除视频外的其他内容,能且只能上传一个视频");
				return jsonResult;
			}
			if(content.split("<embed").length>2){
				jsonResult.setCode("1");
				jsonResult.setErrorDescription("视频资讯内容能且只能上传一个视频,请删除多余的视频再上传");
				return jsonResult;
			}
		}
		news.setContent(content);
		news.setLastModifiedTime(new Date());
		news.setTitle(title);
		news.setType(type);
		news.setPlayLength(playLength);
		try {
			newsService.update(news);
			//修改标题图片
			if(StringUtils.isNotBlank(titleImages)){
				List<Image> images = imageService.getByObjectIdAndCategory(id, ImageCategory.TITLEIMAGE.name());
				Image image = images.get(0);
				image.setLastModifier(Long.valueOf(pcUserId));
				image.setLastModifiedTime(new Date());
				image.setUrl(titleImages);
				imageService.update(image);
			}
		} catch (Exception e) {
			LOGGER.error("error", e);
			e.printStackTrace();
			jsonResult.setCode("1");
			jsonResult.setErrorDescription("修改资讯失败");
			return jsonResult;
		}
		jsonResult.setCode("0");
		jsonResult.setErrorDescription("修改资讯成功");
		return jsonResult;
	}
	
	/**
	 * 上传标题图片
	 * @param file
	 * @return
	 */
	@RequestMapping(value="/uploadFile.do", produces=MediaType.TEXT_PLAIN_VALUE+";charset=utf-8")
	@ResponseBody
	public String uploadFile(HttpServletRequest request) {
		//检验上传文件大小

		//定义允许上传的文件扩展名
		HashMap<String, String> extMap = new HashMap<String, String>();
		extMap.put("image", "gif,jpg,jpeg,png,bmp");
		extMap.put("flash", "swf,flv");
		extMap.put("media", "swf,flv,mp3,MP4,wav,wma,wmv,mid,avi,mpg,asf,rm,rmvb");
		extMap.put("file", "doc,docx,xls,xlsx,ppt,htm,html,txt,zip,rar,gz,bz2");
		String dirName = request.getParameter("dir");
		long maxSize = 1024*1024;
		FileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload upload = new ServletFileUpload(factory);
		upload.setHeaderEncoding("UTF-8");
		if ("image".equals(dirName)) {
			//最大文件大小
			List items=null;
			try {
				items = upload.parseRequest(request);
			} catch (FileUploadException e1) {
				e1.printStackTrace();
			}
			Iterator itr = items.iterator();
			while (itr.hasNext()) {
				FileItem item = (FileItem) itr.next();
				String fileName = item.getName();
				long fileSize = item.getSize();
				if (!item.isFormField()) {
					//检查文件大小
					if(item.getSize() > maxSize){
						return getError("上传文件大小超过限制。");
					}
					//检查扩展名
					String fileExt = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
					if(!allowExt(extMap.get(dirName),fileExt)){
						return getError("上传文件扩展名是不允许的扩展名。\n只允许" + extMap.get(dirName) + "格式。");
					}

					
					String url = "";
					try {
						//构造一个带指定Zone对象的配置类
						Configuration cfg = new Configuration(Zone.zone0());
						//...其他参数参考类注释

						UploadManager uploadManager = new UploadManager(cfg);
						//...生成上传凭证，然后准备上传
						String accessKey = "-qEvZ4_XIN1jzl3tH8GUINoul1dCJ60KxSsIWcto";
						String secretKey = "02oT9kNXrRteTQWEMzqQq6wmnUwPUwfB8ySlgnNa";
						String bucket = "yxin";

						//默认不指定key的情况下，以文件内容的hash值作为文件名
						String key = null;

						try {
						    Auth auth = Auth.create(accessKey, secretKey);
						    String upToken = auth.uploadToken(bucket);

						    try {
						        Response response = uploadManager.put(item.getInputStream(),key,upToken,null, null);
						        //解析上传成功的结果
						        DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
						        System.out.println(putRet.key);
						        System.out.println(putRet.hash);
						        System.out.println(response.url());
						        url = "http://oez2a4f3v.bkt.clouddn.com/"+ putRet.key;
						    } catch (QiniuException ex) {
						        Response r = ex.response;
						        System.err.println(r.toString());
						        try {
						            System.err.println(r.bodyString());
						        } catch (QiniuException ex2) {
						        	ex2.printStackTrace();
									Map result = new HashMap<>();
									result.put("error", 1);
									result.put("message", "图片上传失败");
									return JsonUtils.toJson(result);
						        }
						    }
						} catch (UnsupportedEncodingException ex) {
							ex.printStackTrace();
							Map result = new HashMap<>();
							result.put("error", 1);
							result.put("message", "图片上传失败");
							return JsonUtils.toJson(result);
						}

						Map result = new HashMap<>();
						result.put("error", 0);
						result.put("url", url);
						return JsonUtils.toJson(result);
					} catch (Exception e) {
						e.printStackTrace();
						Map result = new HashMap<>();
						result.put("error", 1);
						result.put("message", "图片上传失败");
						return JsonUtils.toJson(result);
					}

				}
			
			}
			return null;
		}else{
			Set set = setService.get(5L);
			maxSize=Integer.valueOf(set.getContent())*1024*1024;
			List items=null;
			try {
				items = upload.parseRequest(request);
			} catch (FileUploadException e1) {
				e1.printStackTrace();
			}
			Iterator itr = items.iterator();
			while (itr.hasNext()) {
				FileItem item = (FileItem) itr.next();
				String fileName = item.getName();
				long fileSize = item.getSize();
				if (!item.isFormField()) {
					//检查文件大小
					if(item.getSize() > maxSize){
						return getError("上传文件大小超过限制。");
					}
					//检查扩展名
					String fileExt = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
					if(!allowExt(extMap.get(dirName),fileExt)){
						return getError("上传文件扩展名是不允许的扩展名。\n只允许" + extMap.get(dirName) + "格式。");
					}
					
					String url = "";
					try {
						//构造一个带指定Zone对象的配置类
						Configuration cfg = new Configuration(Zone.zone0());
						//...其他参数参考类注释

						UploadManager uploadManager = new UploadManager(cfg);
						//...生成上传凭证，然后准备上传
						String accessKey = "-qEvZ4_XIN1jzl3tH8GUINoul1dCJ60KxSsIWcto";
						String secretKey = "02oT9kNXrRteTQWEMzqQq6wmnUwPUwfB8ySlgnNa";
						String bucket = "yxin";

						//默认不指定key的情况下，以文件内容的hash值作为文件名
						String key = null;

						try {
						    Auth auth = Auth.create(accessKey, secretKey);
						    String upToken = auth.uploadToken(bucket);

						    try {
						        Response response = uploadManager.put(item.getInputStream(),key,upToken,null, null);
						        //解析上传成功的结果
						        DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
						        System.out.println(putRet.key);
						        System.out.println(putRet.hash);
						        System.out.println(response.url());
						        url = "http://oez2a4f3v.bkt.clouddn.com/"+ putRet.key;
						    } catch (QiniuException ex) {
						        Response r = ex.response;
						        System.err.println(r.toString());
						        try {
						            System.err.println(r.bodyString());
						        } catch (QiniuException ex2) {
						        	ex2.printStackTrace();
									Map result = new HashMap<>();
									result.put("error", 1);
									result.put("message", "图片上传失败");
									return JsonUtils.toJson(result);
						        }
						    }
						} catch (UnsupportedEncodingException ex) {
							ex.printStackTrace();
							Map result = new HashMap<>();
							result.put("error", 1);
							result.put("message", "图片上传失败");
							return JsonUtils.toJson(result);
						}

						Map result = new HashMap<>();
						result.put("error", 0);
						result.put("url", url);
						return JsonUtils.toJson(result);
					} catch (Exception e) {
						e.printStackTrace();
						Map result = new HashMap<>();
						result.put("error", 1);
						result.put("message", "图片上传失败");
						return JsonUtils.toJson(result);
					}

				}
			}
			return null;
		}
	}
		private boolean allowExt(String string, String fileExt) {
		String[] split = string.split(",");
		for (String string2 : split) {
			if(string2.equalsIgnoreCase(fileExt)){
				return true;
			}
		}
		return false;
	}
		private String getError(String message) {
			JSONObject obj = new JSONObject();
			obj.put("error", 1);
			obj.put("message", message);
			return obj.toString();
		}
	public static void main(String[] args) {
		String url = "";
		try {
			File file = new File("D:\\文件上传目录\\65320c01-ba57-4f57-a05c-30151330ad8b10.mp4");
			//构造一个带指定Zone对象的配置类
			Configuration cfg = new Configuration(Zone.zone0());
			//...其他参数参考类注释

			UploadManager uploadManager = new UploadManager(cfg);
			//...生成上传凭证，然后准备上传
			String accessKey = "-qEvZ4_XIN1jzl3tH8GUINoul1dCJ60KxSsIWcto";
			String secretKey = "02oT9kNXrRteTQWEMzqQq6wmnUwPUwfB8ySlgnNa";
			String bucket = "yxin";

			//默认不指定key的情况下，以文件内容的hash值作为文件名
			String key = null;
			Auth auth = Auth.create(accessKey, secretKey);
			String upToken = auth.uploadToken(bucket);

			try {
			    Response response = uploadManager.put(new FileInputStream(file),key,upToken,null, null);
			    //解析上传成功的结果
			    DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
			    System.out.println(putRet.key);
			    System.out.println(putRet.hash);
			    System.out.println(response.url());
			    url = "http://oez2a4f3v.bkt.clouddn.com/"+ putRet.key;
			} catch (QiniuException ex) {
			    Response r = ex.response;
			    System.err.println(r.toString());
			    try {
			        System.err.println(r.bodyString());
			    } catch (QiniuException ex2) {
			        //ignore
			    }
			}

			Map result = new HashMap<>();
			result.put("error", 0);
			result.put("url", url);
			System.out.println(JsonUtils.toJson(result));
		} catch (Exception e) {
			e.printStackTrace();
			Map result = new HashMap<>();
			result.put("error", 1);
			result.put("message", "图片上传失败");
			System.out.println(result);
		}
	}
	/**
	 * 获取资讯列表
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="/newsList.do")
	public Map<String, Object> newsList(@RequestParam(value = "page",required=false) String page,
			                      		   @RequestParam(value = "rows",required=false) String rows,
			                      		   @RequestParam(value = "userId",required=false) String userId,
			                      		   @RequestParam(value = "isLocked",required=false) String isLocked,
			                      		   @RequestParam(value = "keyword",required=false) String keyword
			                      		   ){
		PageEntity pageEntity = new PageEntity(Integer.parseInt(page), Integer.parseInt(rows));
		Map<String, Object> map = new HashMap<String, Object>();
		if(StringUtil.isNotEmpty(userId)){
			map.put("userId", userId);
		}
		if(StringUtil.isNotEmpty(isLocked)){
			map.put("isLocked", isLocked);
		}
		if(StringUtil.isNotEmpty(keyword)){
			map.put("keyword", keyword);
		}
		pageEntity.setParams(map);
		PagingResult<NewsDto> newslist = newsService.selectParamForBack(pageEntity);
		filterImgs(newslist);
		List<NewsDto> news = newslist.getResultList();
		Map result = new HashMap();
		result.put("rows", news);
		result.put("total", Integer.valueOf(newslist.getTotalSize()));
		return result;
	}
	/*
	 * 加工图集
	 */
	private void filterImgs(PagingResult<NewsDto> newslist) {
		List<NewsDto> resultList = newslist.getResultList();
		if(resultList!=null){
			for (int i = 0; i < resultList.size(); i++) {
				NewsDto newsIndex = resultList.get(i);
				if("Imgs".equals(newsIndex.getImgOrVedio())){
					Map paramMap=new HashMap<>();
					paramMap.put("newsId", newsIndex.getId());
					List<ImgsItem> imgsItemList = imgsItemMapper.selectParam(paramMap);
					newsIndex.setImgsItems(imgsItemList);
				}
			}
		}
	}
	/**
	 * 锁定/解锁资讯
	 * @return
	 * @throws Exception 
	 */
	@ResponseBody
	@RequestMapping(value="/updateNewsStatus.do")
	public Map<String, Object> updateNewsStatus(@RequestParam(value = "id",required=false) Long id,
			@RequestParam(value = "status",required=false) String status
			) throws Exception{
		JSONObject result=new JSONObject();
		News news = newsService.get(id);
		news.setIsLocked("1".equals(status)?true:false);
		newsService.update(news);
		if("1".equals(status)){
			//推送屏蔽消息
			AppMessage appMessage = new AppMessage();
			appMessage.setPushtime(new Date());
			appMessage.setIsread(0);
			appMessage.setUserid(news.getUserId());
			appMessage.setType(PushConst.NEWS);
			appMessage.setTitle("资讯屏蔽");
			appMessage.setContent("您发布的标题为      "+news.getTitle()+"  的资讯,因内容违规已被屏蔽");
			appMessageService.pushMessage(appMessage);
		}
		result.put("success", true);
		return result;
	}
	/**
	 * 推送系统消息
	 */
	@ResponseBody
	@RequestMapping(value="/sysNewsPush.do")
	public Map<String, Object> sysNewsPush(@RequestParam(value = "title",required=true) String title,
			@RequestParam(value = "content",required=true) String content
			) throws Exception{
		JSONObject result=new JSONObject();
		//推送系统消息给所有在线用户(deviceToken不为null的用户)
		Map paramMap = new HashMap<>();
		paramMap.put("deviceTokenNotNull","deviceTokenNotNull");
		List<UserOne> pushUsers = appUserOneService.selectParam(paramMap);
		if(pushUsers!=null&&!pushUsers.isEmpty()){
			for (int i = 0; i < pushUsers.size(); i++) {
				UserOne pushUser = pushUsers.get(i);
				AppMessage appMessage = new AppMessage();
				appMessage.setPushtime(new Date());
				appMessage.setIsread(0);
				appMessage.setUserid((long)pushUser.getUser_id());
				appMessage.setType(PushConst.NEWS);
				appMessage.setTitle(title);
				appMessage.setContent(content);
				appMessageService.pushMessage(appMessage);
			}
		}
		result.put("success", true);
		return result;
	}
	/**
	 * 推送系统消息页面
	 */
	@RequestMapping(value="/sysNewsPushManage.do")
	public String sysNewsPushManage() throws Exception{
		return "sysNewsPushManage";
	}
	/**
	 * 置顶/推荐资讯
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="/updateNewsLable.do")
	public Map<String, Object> updateNewsLable(@RequestParam(value = "id",required=false) Long id,
			@RequestParam(value = "lable",required=false) String lable
			){
		JSONObject result=new JSONObject();
		News news = newsService.get(id);
		if("0".equals(lable)){
			news.setOrderNo(2);
		}else if("1".equals(lable)){
			news.setOrderNo(1);
		}else{
			news.setOrderNo(0);
		}
		news.setLable(lable);
		newsService.update(news);
		result.put("success", true);
		return result;
	}
	/**
	 * 批量删除资讯
	 */
	@ResponseBody
	@RequestMapping(value="/deleteNews.do")
	public Map<String, Object> deleteNews(Long[] delIds){
		JSONObject result=new JSONObject();
//		newsService.deleteBatch(Arrays.asList(delIds));
//		//删除相应子表
//		//如果是Image类型,删除标题图片
//		for (Long newsId : delIds) {
//			List<Image> byObjectIdAndCategory = imageService.getByObjectIdAndCategory(newsId, ImageCategory.TITLEIMAGE.name());
//			if(byObjectIdAndCategory!=null&&byObjectIdAndCategory.size()>0){
//				Map paramMap=new HashMap<>();
//				paramMap.put("collectId", byObjectIdAndCategory.get(0).getId());
//				imageService.deleteParam(paramMap);
//			}
//		}
//		//删除该资讯的点赞记录
//		for (Long newsId : delIds) {
//			Map paramMap=new HashMap<>();
//			paramMap.put("newsId", newsId);
//			paramMap.put("category", ImageCategory.NEWS_DETAIL.name());
//			List<Islike> islikeList= islikeService.getByNewsIdAndCategory(paramMap);
//			if(islikeList!=null&&islikeList.size()>0){
//				for (Islike islike : islikeList) {
//					islikeService.delete(islike.getId());
//				}
//			}
//		}
//		//删除该资讯的评论
//		for (Long newsId : delIds) {
//			List<Evaluate> byObjectIdAndCategory = evaluateService.getByObjectIdAndCategory(newsId, null, ImageCategory.NEWS.name());
//			//先删除每条评论的点赞记录
//			if(byObjectIdAndCategory!=null&&byObjectIdAndCategory.size()>0){
//				for (Evaluate evaluate : byObjectIdAndCategory) {
//					Map paramMap=new HashMap<>();
//					paramMap.put("newsId", evaluate.getId());
//					paramMap.put("category", ImageCategory.NEWS.name());
//					List<Islike> islikeList= islikeService.getByNewsIdAndCategory(paramMap);
//					if(islikeList!=null&&islikeList.size()>0){
//						for (Islike islike : islikeList) {
//							islikeService.delete(islike.getId());
//						}
//					}
//					evaluateService.delete(evaluate.getId());
//				}
//			}
//		}
		for (Long id : delIds) {
			newsService.deleteNews(id);
		}
		result.put("success", true);
		result.put("delNums", delIds.length);
		return result;
	}
	/**
	 * 资讯类型设置页面
	 */
	@RequestMapping(value="/newsTypeManage.do")
	public String newsTypeManage(){
		return "newsTypeManage";
	}
	/**
	 * 新增资讯类型
	 */
	@ResponseBody
	@RequestMapping(value="/insertNewsType.do")
	public Map<String, Object> insertNewsType(String name,String code,int orderNo){
		JSONObject result=new JSONObject();
		Map map = new HashMap<>();
		//编号不能重复
		map.put("name", name);
		NewsType newsType=newsMapper.getNewsTypeParam(map);
		if(newsType!=null){
			result.put("success", false);
			result.put("msg", "名称已存在");
			return result;
		}
		map.clear();
		map.put("code", code);
		newsType=newsMapper.getNewsTypeParam(map);
		if(newsType!=null){
			result.put("success", false);
			result.put("msg", "编号已存在");
			return result;
		}
		map.clear();
		map.put("orderNo",orderNo);
		newsType=newsMapper.getNewsTypeParam(map);
		if(newsType!=null){
			result.put("success", false);
			result.put("msg", "序号已存在");
			return result;
		}
		map.put("name", name);
		map.put("code", code);
		map.put("orderNo",orderNo);
		map.put("createdTime", new Date());
		newsService.insertNewsType(map);
		result.put("success", true);
		return result;
	}
	/**
	 * 批量删除资讯类型
	 */
	@ResponseBody
	@RequestMapping(value="/deleteNewsType.do")
	public Map<String, Object> deleteNewsType(Long[] delIds){
		JSONObject result=new JSONObject();
		newsService.deleteNewsType(Arrays.asList(delIds));
		result.put("success", true);
		result.put("delNums", delIds.length);
		return result;
	}
	/**
	 * 修改资讯类型
	 */
	@ResponseBody
	@RequestMapping(value="/updateNewsType.do")
	public Map<String, Object> updateNewsType(Long id,String name,String code,int orderNo){
		JSONObject result=new JSONObject();
		Map map = new HashMap<>();
		//编号不能重复
		map.put("id", id);
		map.put("name", name);
		NewsType newsType=newsMapper.getNewsTypeParam(map);
		if(newsType!=null){
			result.put("success", false);
			result.put("msg", "名称已存在");
			return result;
		}
		map.clear();
		map.put("id", id);
		map.put("code", code);
		newsType=newsMapper.getNewsTypeParam(map);
		if(newsType!=null){
			result.put("success", false);
			result.put("msg", "编号已存在");
			return result;
		}
		map.clear();
		map.put("id", id);
		map.put("orderNo",orderNo);
		newsType=newsMapper.getNewsTypeParam(map);
		if(newsType!=null){
			result.put("success", false);
			result.put("msg", "序号已存在");
			return result;
		}
		map.put("id", id);
		map.put("name", name);
		map.put("code", code);
		map.put("orderNo",orderNo);
		newsService.updateNewsType(map);
		result.put("success", true);
		return result;
	}
	/**
	 * 根据id查询资讯类型
	 */
	@ResponseBody
	@RequestMapping(value="/getNewsType.do")
	public Map<String, Object> getNewsType(Long id){
		JSONObject result=new JSONObject();
		Map map = new HashMap<>();
		map.put("id", id);
		Map newsType = newsService.getNewsType(map);
		return newsType;
	}
	/**
	 * 获取所有资讯类型
	 */
	@ResponseBody
	@RequestMapping(value="/getAllNewsType.do")
	public List<Map> getAllNewsType(){
		JSONObject result=new JSONObject();
		List<Map> allNewsType = newsService.getAllNewsType();
		for (int i = 0; i < allNewsType.size(); i++) {
			Map map = allNewsType.get(i);
			if((long)map.get("id")==1||(long)map.get("id")==2){
				allNewsType.remove(i);
				i--;
			}
		}
		return allNewsType;
	}
	
	
	
}
