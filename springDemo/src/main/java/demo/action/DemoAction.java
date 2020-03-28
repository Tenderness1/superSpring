package demo.action;

import demo.mvcframwork.annotation.SuperController;
import demo.mvcframwork.annotation.SuperRequestMapping;
import demo.mvcframwork.annotation.SuperRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@SuperController
@SuperRequestMapping("/demo")
public class DemoAction {
    @SuperRequestMapping("/query.json")
    public void query(HttpServletRequest req, HttpServletResponse res , @SuperRequestParam("name") String name) {
        try {
            res.getWriter().write(name);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    @SuperRequestMapping("/add.json")
    public void add(HttpServletRequest req, HttpServletResponse res , @SuperRequestParam("a") Integer a,@SuperRequestParam("b") Integer b) {
        try {
            res.getWriter().write(a+"+"+b+"="+(a+b));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    @SuperRequestMapping("/remove.json")
    public void remove(HttpServletRequest req, HttpServletResponse res , @SuperRequestParam("id") String id) {
        try {
            res.getWriter().write(id);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
