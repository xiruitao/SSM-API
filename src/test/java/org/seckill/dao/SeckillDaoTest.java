package org.seckill.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.seckill.entity.Seckill;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 配置spring和junit整合，junit启动时加载springIOC容器
 * spring-test,junit
 * 告诉 junit spring配置文件
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring/spring-dao.xml"})
public class SeckillDaoTest {

    //注入Dao实现类依赖
    @Resource
    private SeckillDao seckillDao;

    @Test
    public void testReduceNumber() {
        Date killTime = new Date();
        int updateCount = seckillDao.reduceNumber(1000L,killTime);
        System.out.println(updateCount);
        /**
         * - ==>Preparing: update seckill set number = number -1 where seckill_id = ? and start_time <= ? and end_time >= ? and number >0;
         * - ==>Parameters: 1000(Long), 2019-02-17 14:42:29.088(Timestamp), 2019-02-17 14:42:29.088(Timestamp)
         * - <==    Updates: 0
         */
    }

    @Test
    public void testQueryById() throws Exception{
        long id = 1000;
        Seckill seckill = seckillDao.queryById(id);
        System.out.println(seckill.getName());
        System.out.println(seckill);
        /**
         * 2000元秒杀iPhoneX
         * Seckill{seckillId=1000, name='2000元秒杀iPhoneX', number=100, startTime=Thu Feb 14 08:00:00 CST 2019, endTime=Sat Feb 16 08:00:00 CST 2019, createTime=Sun Feb 17 03:58:02 CST 2019}
         */
    }

    @Test
    public void testQueryAll() throws Exception{
        List<Seckill> seckillList = seckillDao.queryAll(0,100);
        for (Seckill seckill:seckillList){
            System.out.println(seckill);
        }
        /**
         * Seckill{seckillId=1000, name='2000元秒杀iPhoneX', number=100, startTime=Thu Feb 14 08:00:00 CST 2019, endTime=Sat Feb 16 08:00:00 CST 2019, createTime=Sun Feb 17 03:58:02 CST 2019}
         * Seckill{seckillId=1001, name='1000元秒杀oppo20', number=200, startTime=Thu Feb 14 08:00:00 CST 2019, endTime=Sat Feb 16 08:00:00 CST 2019, createTime=Sun Feb 17 03:58:02 CST 2019}
         * Seckill{seckillId=1002, name='2000元秒杀vivo20', number=300, startTime=Thu Feb 14 08:00:00 CST 2019, endTime=Sat Feb 16 08:00:00 CST 2019, createTime=Sun Feb 17 03:58:02 CST 2019}
         * Seckill{seckillId=1003, name='2000元秒杀小米8', number=400, startTime=Thu Feb 14 08:00:00 CST 2019, endTime=Sat Feb 16 08:00:00 CST 2019, createTime=Sun Feb 17 03:58:02 CST 2019}
         */
    }
}