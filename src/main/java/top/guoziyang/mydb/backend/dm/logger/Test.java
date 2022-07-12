package top.guoziyang.mydb.backend.dm.logger;

import sun.applet.Main;

import java.util.Scanner;

public class Test {
    static class Node{
        int num;
        Node next;
        Node(int x){
            this.num=x;
        }
    }
    static Scanner in=new Scanner(System.in);
    public static void main(String[] args) {

        Node head=new Node(0);
        Node cur=head;
        for(int i=0;i<5;i++){
            create(cur);
            cur=cur.next;
        }

        dayin(head);

        
    }
    public static  void create(Node cur){
        int value=in.nextInt();
        Node node=new Node(value);//值为value的一个节点

        cur.next=node;
    }


    //dayin
    public static void dayin(Node head){
        Node temp=head;
        while(temp!=null){
            System.out.print(temp.num+" ");
            temp=temp.next;
        }
    }
    
}
