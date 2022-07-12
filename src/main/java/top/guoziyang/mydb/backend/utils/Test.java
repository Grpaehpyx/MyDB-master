package top.guoziyang.mydb.backend.utils;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class Test {
    public static void main(String[] args) throws  AWTException {

        AbstractQueuedSynchronizer abstractQueuedSynchronizer=new AbstractQueuedSynchronizer() {
            @Override
            protected boolean tryAcquire(int arg) {
                return super.tryAcquire(arg);
            }
        };
                Random random=new Random();
//        前期准备
                Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
                Transferable tText = null;
                Toolkit tolkit = Toolkit.getDefaultToolkit();
                Robot robot = new Robot();
                //剪切板内容，要发的内容多的话，可以补充
                String[] lists={"517应到6人，实到5人，一人请假回家一直未归。"};

                //三秒后开始开始执行
                robot.delay(3000);
                while (true) {
//            接收要发的内容这里仅有一条消息，直接接收第0条数据
                    tText = new StringSelection(lists[0]);
                    clip.setContents(tText, null);
                    robot.keyPress( KeyEvent.VK_CONTROL);
                    robot.keyPress(KeyEvent.VK_V);
//            粘贴文本信息
                    robot.keyRelease(KeyEvent.VK_CONTROL);
//            发送信息
                    robot.keyPress( KeyEvent.VK_ENTER);
                    robot.delay(1000);
//
                 }


    }
}
