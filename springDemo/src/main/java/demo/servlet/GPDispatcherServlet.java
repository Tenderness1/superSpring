package demo.servlet;

import demo.mvcframwork.annotation.SuperAutowired;
import demo.mvcframwork.annotation.SuperController;
import demo.mvcframwork.annotation.SuperRequestMapping;
import demo.mvcframwork.annotation.SuperSerivce;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class GPDispatcherServlet extends HttpServlet {
    private static final long serialVersionUID=1L;
    //用web.xml中的param-name 的值一致
    private static final String LOCATION="ContentConfigLocation";
    //保存所有的配置信息
    private Properties p=new Properties();
    //保存所有被扫描到的相关的类名
    private List<String> className = new ArrayList<String>();
    //核心IOC容器 保存所有初始化的Bean
    private Map<String, Object> ioc = new HashMap<String, Object>();

    //保存所有的Url和方法的映射关系
    private Map<String, Method> handleMapping = new HashMap<String, Method>();

    public GPDispatcherServlet(){
        super();
    }

    /**
     * 初始化加载配置文件
     * @param config
     * @throws ServletException
     */
    public void init(ServletConfig config)throws ServletException{

        //1加载配置文件
        doLoadConfig(config.getInitParameter(LOCATION));

        //2扫描所有相关的类
        doScanner(p.getProperty("scanPackage"));

        //3 初始化所有相关类的实例，并保存到IOC容器中
        doInstance();
        //4依赖注入
        doAutoWired();
        //5.构造HandlerMapping
        initHadleMapping();

        //6 等待请求，匹配URL，定位方法， 反射调用执行
        //调用doGet 或者doPost方法

        ///提示信息
        System.out.println("超级spring框架已经启动了");
    }

    private void initHadleMapping() {
        if (ioc.isEmpty()){
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(SuperController.class)) {
                continue;
            }
            String baseUrl="";
            //获取Controller的url设置
            if (clazz.isAnnotationPresent(SuperRequestMapping.class)) {
                SuperRequestMapping requestMapping = clazz.getAnnotation(SuperRequestMapping.class);
                baseUrl = requestMapping.value();
            }
    //获取Method的url配置
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                //没有加Requstmappomg的直接忽略
                if (!method.isAnnotationPresent(SuperRequestMapping.class)) {
                    continue;
                }
                //映射URL
                SuperRequestMapping requestMapping = method.getAnnotation(SuperRequestMapping.class);
                String url=(baseUrl+requestMapping.value().replaceAll("/+","/"));
                handleMapping.put(url,method);
                System.out.println("mapped"+url+","+method);
            }
        }
    }

    private void doAutoWired() {
        if (ioc.isEmpty()){
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //拿到实例对象中的所有属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(SuperAutowired.class)){
                    continue;
                }
                SuperAutowired autowired = field.getAnnotation(SuperAutowired.class);
                String beanName = autowired.value().trim();
                if ("".equals(beanName)){
                    beanName=field.getType().getName();
                }
                try {
                    field.set(entry.getValue(),ioc.get(beanName));
                }catch (Exception e){
                    e.printStackTrace();
                    continue;
                }

            }
        }
    }

    /**
     * 首字母小写
     */
    public String lowerFirstCase(String str){
        char [] chars=str.toCharArray();
        chars[0]+=32;
        return String.valueOf(chars);
    }
    private void doInstance() {
        if (className.size()==0){
            return;
        }
        try {

        for (String className : className) {
            Class<?> clazz = Class.forName(className);
            if (clazz.isAnnotationPresent(SuperController.class)){
                //默认将首字母小写作为beanName
                String beanName = lowerFirstCase(clazz.getSimpleName());
                ioc.put(beanName,clazz.newInstance());
            }else if (clazz.isAnnotationPresent(SuperSerivce.class)){
                SuperSerivce serivce = clazz.getAnnotation(SuperSerivce.class);
                String beanName = serivce.value();
                //如果用户设置了名字 就用该客户自己的设置
                if (!"".equals(beanName.trim())){
                    ioc.put(beanName,clazz.newInstance());
                    continue;
                }
                //如果自己设置 就按接口类型的创建一个实例
                Class<?>[] interfaces = clazz.getInterfaces();
                for (Class<?> i : interfaces) {
                    ioc.put(i.getName(), clazz.newInstance());
                }
            }else {
                continue;
            }
        }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void doScanner(String packageName) {
        //将所有的包路径法转换为文件路径
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            //如果是文件夹 继续递归
            if (file.isDirectory()){
                doScanner(packageName+"."+file.getName());
            }else {
                className.add(packageName + "." + file.getName().replace(".class", "").trim());
            }
        }

    }

    private void doLoadConfig(String location) {
        InputStream fis=null;
        fis=this.getClass().getClassLoader().getResourceAsStream(location);
        //1 读取配置文件
        try {
            p.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (null!=fis){
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    /**
     * 执行业务处理
     * @param req
     * @param resp
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            doDispatch(req,resp);//开始匹配对应的方法
        }catch (Exception e){
            //匹配出现异常
            resp.getWriter().write("50000"+Arrays.toString(e.getStackTrace()).replaceAll("\\[|\\]]","").replaceAll(",\\s","\r\n"));
        }

    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (this.handleMapping.isEmpty()) {
            return;
        }
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");
        if (!this.handleMapping.containsKey(url)) {
            resp.getWriter().write("404 not Found");
            return;
        }
        Map<String, String[]> params = req.getParameterMap();
        Method method = this.handleMapping.get(url);
        //获取方法的参数
        Class<?>[] parameterTypes = method.getParameterTypes();
        //获取请求的参数
        Map<String, String[]> parameterMap = req.getParameterMap();
        //保存参数的值
        Object[] paramValues = new Object[parameterTypes.length];
        //方法的参数列表
        for (int i = 0; i < parameterTypes.length; i++) {
            //根据参数名称 做某些处理
            Class<?> parameterType = parameterTypes[i];
            if (parameterType == HttpServletRequest.class) {
                //参数类型已明确 这边强制类型
                paramValues[i] = req;
                continue;
            } else if (parameterType == HttpServletResponse.class) {
                paramValues[i] = resp;
                continue;
            } else if (parameterType == String.class) {
                for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
                    String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll("\\s", "\r\n");
                    paramValues[i] = value;
                }
            }
        }

        String beanName = lowerFirstCase(method.getDeclaringClass().getSimpleName());
        //利用反射机制来调用
        method.invoke(this.ioc.get(beanName), paramValues);
    }
}
