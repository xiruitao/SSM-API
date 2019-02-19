package org.seckill.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.seckill.entity.SuccessKilled;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring/spring-dao.xml"})
public class SuccessKilledDaoTest {

    @Resource
    private SuccessKilledDao successKilledDao;

    @Test
    public void testInsertSuccessKilled() throws Exception{
        long id = 1000L;
        long phone = 123456789L;
        int insertCount = successKilledDao.insertSuccessKilled(id,phone);
        System.out.println("insertCount" + insertCount);
        /**
         * - ==>  Preparing: insert ignore into success_killed(seckill_id,user_phone,state) values (?,?,0)
         * - ==> Parameters: 1000(Long), 123456789(Long)
         * - <==    Updates: 1
         */
    }

    @Test
    public void testQueryByIdWithSeckill() throws Exception{
        long id = 1000L;
        long phone = 123456789L;
        SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(id,phone);
        System.out.println(successKilled);
        System.out.println(successKilled.getSeckill());
        /**
         * - ==>  Preparing: select sk.seckill_id, sk.user_phone as userPhone, sk.create_time, sk.state, s.seckill_id "seckill.seckill_id", s.name "seckill.name", s.number "seckill.number", s.start_time "seckill.start_time", s.end_time "seckill.end_time", s.create_time "seckill.create_time" from success_killed sk inner join seckill s on sk.seckill_id = s.seckill_id where sk.seckill_id=? and sk.user_phone=?
         * - ==> Parameters: 1000(Long), 123456789(Long)
         * - <==      Total: 1
         * 15:38:17.867 [main] DEBUG org.mybatis.spring.SqlSessionUtils - Closing non transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@7283d3eb]
         * 15:38:17.867 [main] DEBUG org.springframework.jdbc.datasource.DataSourceUtils - Returning JDBC Connection to DataSource
         *
         * SuccessKilled{
         * seckillId=1000,
         * userPhone=123456789,
         * state=0,
         * createTime=Sun Feb 17 23:34:11 CST 2019}
         *
         * Seckill{
         * seckillId=1000,
         * name='2000元秒杀iPhoneX',
         * number=100,
         * startTime=Thu Feb 14 08:00:00 CST 2019,
         * endTime=Sat Feb 16 08:00:00 CST 2019,
         * createTime=Sun Feb 17 03:58:02 CST 2019}
         */
    }
}