 /* Copyright zeping lu
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the License for the specific language governing permissions and
  *  limitations under the License.
  */

 package com.lzp.zprpc.common.util;


 import java.util.AbstractQueue;
 import java.util.Collection;
 import java.util.Iterator;
 import java.util.concurrent.BlockingQueue;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.AtomicInteger;

 /**
  * Description:
  * 相比{@link java.util.concurrent.ArrayBlockingQueue}优势
  * 性能高：
  * 1、ArrayBlockingQueue取元素和放元素用的是同一把锁，多线程同时读写时严重影响性能
  * 而我这个这个队列取元素和放元素锁的是不同的对象
  * 2、ArrayBlockingQueue用的是JUC里的Lock锁，而我这个用的是synchronized锁
  *   synchronized相比于Lock锁的优势
  *   (1)内存占用少：synchronized锁标记是在对象头里,所以任何对象都能当成锁,不需要额外new锁对象。
  *   (2)性能不比Lock锁差：经过实测,不管线程竞争是否激烈,关闭JIT的情况下，synchronized性能远远
  *   高于Lock锁,开启JIT的情况下,synchronized性能相比Lock也差不了多少。(JUC里的Lock锁由于是
  *   java代码实现的,所以能更大地享受到JIT的收益)
  *
  * @author: Zeping Lu
  * @date: 2020/11/10 17:15
  */
 public class OptimizedArrBlockQueue<E> extends AbstractQueue<E>
         implements BlockingQueue<E>, java.io.Serializable {

     private class Itr implements Iterator<E> {

         private Object[] elementDataView;
         private int index = 0;

         {
             synchronized (this) {
                 synchronized (items) {
                     int temp;
                     if ((temp = takeIndex - putIndex) < 0) {
                         elementDataView = new Object[-temp];
                         System.arraycopy(items, takeIndex, elementDataView, 0, -temp);
                     } else if (temp == 0) {
                         if (items[putIndex] == null) {
                             elementDataView = new Object[0];
                         } else {
                             elementDataView = new Object[items.length];
                             System.arraycopy(items, takeIndex, elementDataView, 0, items.length - takeIndex);
                             System.arraycopy(items, 0, elementDataView, putIndex + 1, takeIndex);
                         }
                     } else {
                         elementDataView = new Object[items.length - temp];
                         System.arraycopy(items, takeIndex, elementDataView, 0, items.length - takeIndex);
                         System.arraycopy(items, 0, elementDataView, items.length - takeIndex, putIndex);
                     }
                 }
             }
         }

         @Override
         public boolean hasNext() {
             return index != elementDataView.length;
         }

         @Override
         public E next() {
             return (E) elementDataView[index++];
         }
     }

     private int takeIndex;
     /**
      * 存元素的容器，并且作为读锁。
      */
     private transient final Object[] items;

     private final Object PUT_WAITING_LOCK = new Object();

     private final Object TAKE_WAITING_LOCK = new Object();

     private final AtomicInteger size = new AtomicInteger();

     private int putIndex;

     public OptimizedArrBlockQueue(int capacity) {
         this.items = new Object[capacity];
     }


     @Override
     public Iterator<E> iterator() {
         return new Itr();
     }

     @Override
     public synchronized int size() {
         synchronized (items) {
             int temp;
             if ((temp = takeIndex - putIndex) < 0) {
                 return -temp;
             } else if (temp == 0) {
                 return items[putIndex] == null ? 0 : items.length;
             }
             return items.length - temp;
         }
     }

     @Override
     public synchronized void put(E e) throws InterruptedException {
         if (e == null) {
             throw new NullPointerException();
         }
         Object[] items = this.items;
         //这个操作能保证元素放取的可见性
         while (size.get() == items.length) {
             if (putSpinSucceed()) {
                 break;
             }
             synchronized (PUT_WAITING_LOCK) {
                 if (size.get() != items.length) {
                     break;
                 }
                 PUT_WAITING_LOCK.wait();
             }
         }
         enqueue(e, items);
     }

     @Override
     public synchronized boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
         if (e == null) {
             throw new NullPointerException();
         }
         Object[] items = this.items;
         long remainingTime = unit.toMillis(timeout);
         while (size.get() == items.length) {
             if (remainingTime<0){
                 return false;
             }
             if (putSpinSucceed()) {
                 break;
             }
             synchronized (PUT_WAITING_LOCK) {
                 if (size.get() != items.length) {
                     break;
                 }
                 long deadline = System.currentTimeMillis() + remainingTime;
                 PUT_WAITING_LOCK.wait(remainingTime);
                 if (size.get() != items.length) {
                     break;
                 } else {
                     remainingTime = deadline - System.currentTimeMillis();
                 }
             }
         }
         enqueue(e, items);
         return true;
     }


     private boolean putSpinSucceed(){
         boolean notFull = false;
         for (int i = 0; i < 10; i++) {
             if (size.get() != items.length) {
                 notFull = true;
                 break;
             }
             Thread.yield();
         }
         return notFull;
     }

     @Override
     public E take() throws InterruptedException {
         Object[] items = this.items;
         synchronized (items) {
             E e;
             //这个操作能保证元素放取的可见性
             while (size.get() == 0) {
                 if (takeSpinSucceed()){
                     break;
                 }
                 synchronized (TAKE_WAITING_LOCK) {
                     if (size.get() != 0) {
                         break;
                     }
                     TAKE_WAITING_LOCK.wait();
                 }
             }
             e = (E) items[takeIndex];
             dequeue(items);
             return e;
         }
     }

     private boolean takeSpinSucceed(){
         boolean notEmpty = false;
         for (int i = 0; i <10 ; i++) {
             if (size.get() != 0) {
                 notEmpty  = true;
                 break;
             }
             Thread.yield();
         }
         return notEmpty;
     }

     @Override
     public E poll(long timeout, TimeUnit unit) throws InterruptedException {
         synchronized (items) {
             long remainingTime = unit.toMillis(timeout);
             while (size.get() == 0) {
                 if (remainingTime < 0) {
                     return null;
                 }
                 if (takeSpinSucceed()){
                     break;
                 }
                 synchronized (TAKE_WAITING_LOCK) {
                     if (size.get() != 0) {
                         break;
                     }
                     long deadline = System.currentTimeMillis() + remainingTime;
                     TAKE_WAITING_LOCK.wait(remainingTime);
                     if (size.get() != 0) {
                         break;
                     } else {
                         remainingTime = deadline - System.currentTimeMillis();
                     }
                 }
             }
             E e = (E) items[takeIndex];
             dequeue(items);
             return e;
         }
     }

     @Override
     public synchronized int remainingCapacity() {
         synchronized (items) {
             int temp;
             if ((temp = putIndex - takeIndex) > 0) {
                 return items.length - temp;
             } else if (temp == 0) {
                 return items[putIndex] == null ? items.length : 0;
             } else {
                 return -temp;
             }
         }
     }

     @Override
     public int drainTo(Collection<? super E> c) {
         return 0;
     }

     @Override
     public int drainTo(Collection<? super E> c, int maxElements) {
         return 0;
     }

     @Override
     public synchronized boolean offer(E e) {
         if (e == null) {
             throw new NullPointerException();
         }
         if (size.get() == items.length) {
             return false;
         }
         enqueue(e, items);
         return true;
     }

     @Override
     public E poll() {
         synchronized (items) {
             E e;
             if ((e = (E) items[takeIndex]) == null) {
                 return null;
             }
             dequeue(items);
             return e;
         }
     }

     @Override
     public E peek() {
         throw new UnsupportedOperationException();
     }

     private void enqueue(E e, Object[] items) {
         items[putIndex++] = e;
         if (size.getAndIncrement() == 0) {
             synchronized (TAKE_WAITING_LOCK) {
                 TAKE_WAITING_LOCK.notify();
             }
         }
         if (putIndex == items.length) {
             putIndex = 0;
         }
     }

     private void dequeue(Object[] items) {
         items[takeIndex++] = null;
         if (size.getAndDecrement() == items.length) {
             synchronized (PUT_WAITING_LOCK) {
                 PUT_WAITING_LOCK.notify();
             }
         }
         if (takeIndex == items.length) {
             takeIndex = 0;
         }
     }
 }
