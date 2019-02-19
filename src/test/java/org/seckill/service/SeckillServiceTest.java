package org.seckill.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.seckill.dto.Exposer;
import org.seckill.dto.SeckillExecution;
import org.seckill.entity.Seckill;
import org.seckill.exception.RepeatKillException;
import org.seckill.exception.SeckillCloseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring/spring-dao.xml",
        "classpath:spring/spring-service.xml"})
public class SeckillServiceTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillService seckillService;

    @Test
    public void testGetSeckillList() throws Exception{
        List<Seckill> list = seckillService.getSeckillList();
        logger.info("list={}",list);
    }

    @Test
    public void testGetById() throws Exception{
        long id = 1000;
        Seckill seckill = seckillService.getById(id);
        logger.info("seckill={}",seckill);
    }

    @Test
    public void testExportSeckillUrl() throws Exception{
        long id = 1001;
        Exposer exposer = seckillService.exportSeckillUrl(id);
        logger.warn("exposer={}", exposer);
        /**
         * exposer=Exposer{
         * exposed=true,
         * md5='412de575ac685c468862d4cd59550eca',
         * seckillId=1001,
         * now=0, start=0, end=0}
         */
    }

    @Test
    public void testExecuteSeckill()throws Exception{
        long id = 1001;
        long phone = 1234567890;
        String md5 = "412de575ac685c468862d4cd59550eca";//每次都会改变
        try{
            SeckillExecution execution = seckillService.executeSeckill(id,phone,md5);
            logger.info("result={}",execution);
        }catch (RepeatKillException e){
            logger.error(e.getMessage());
        }catch (SeckillCloseException e){
            logger.error(e.getMessage());
        }
    }
    @Test
    public void testSeckillLogic() throws Exception{
        long id = 1000;
        Exposer exposer = seckillService.exportSeckillUrl(id);
        if (exposer.isExposed()){
            logger.info("exposer={}", exposer);
            long userPhone = 12345678901L;
            String md5= exposer.getMd5();
            try {
                SeckillExecution seckillExecution = seckillService.executeSeckill(id, userPhone, md5);
                logger.info("result={}", seckillExecution);
            } catch (RepeatKillException e) {
                logger.error(e.getMessage());
            } catch (SeckillCloseException e) {
                logger.error(e.getMessage());
            }

        }else{
            // 秒杀未开启
            logger.warn("exposer={}",exposer);
        }
    }

    @Test
    public void executeSeckillProcedure(){
        long seckillId = 1001;
        long userPhone =  15895876329L;
        Exposer exposer = seckillService.exportSeckillUrl(seckillId);
        if (exposer.isExposed()){
            String md5 = exposer.getMd5();
           SeckillExecution execution = seckillService.executeSeckillProcedure(seckillId,userPhone,md5);
            logger.info(execution.getStateInfo());
        }
    }
}