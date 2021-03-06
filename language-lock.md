锁是一个常见的同步概念，加锁（lock）或者解锁（unlock），or获取（acquire）和释放（release）。



## 自旋锁（spinlock）
spin更为通俗的一个词是busy waiting，其实就是死循环

```
while (抢锁(lock) == 没抢到) {

}

```
只要没有锁上/抢到锁，就不断重试。

缺点是：如果别的线程长期持有该锁，那么当前线程就一直重复检查是否能够加锁，浪费 CPU。

因此想到：没有必要一直去尝试加锁，抢锁失败后让出 CPU 给别的线程先执行，这就是互斥（mutex）

## 互斥锁(mutex/mutual exclusive)

```
while (抢锁(lock) == 没抢到) {
    sleep当前线程, 在这把锁的状态发生改变时再唤醒(lock);
}
```

操作系统负责线程调度，为了实现`锁的状态发生改变时再唤醒`就需要把锁也交给操作系统管理。

所以mutex的加锁操作通常都需要涉及到上下文切换，操作花销也就会比自旋锁要大。

以上两者的作用是加锁互斥，保证能够`exclusively`地访问被锁保护的资源。


## 条件变量（condition variable）/条件锁

并不是所有场景下我们都希望能够独占某个资源，比如 Producer-consumer problem /Bounded-buffer problem

```
// The custumer:

加锁(lock);  // lock 保护对 queue 的操作
while (queue.isEmpty()) {  // 队列为空时等待
    解锁(lock);
    // 这里让出锁，让生产者有机会往 queue 里安放数据
    加锁(lock);
}
data = queue.pop();  // 至此肯定非空，所以能对资源进行操作
解锁(lock);
消费(data);  // 在临界区外做其它处理
```

while (queue.isEmpty()) 相当于又搞了一个自旋锁， 区别在于这次不是在 while 一个抽象资源是否可用，而是在 while 某个被锁保护的具体的条件是否达成。

有了前面自旋锁、互斥器的经验就不难想到："只要条件没有发生改变，while 里就没有必要再去加锁、判断、条件不成立、解锁，完全可以让出 CPU 给别的线程"。
不过由于「条件是否达成」属于业务逻辑，操作系统没法管理，需要让能够作出这一改变的代码来手动通知，
比如上面的例子里就需要在生产者往 queue 里 push 后通知 !queue.isEmpty() 成立。

也就是说，我们希望把上面例子中的 while 循环变成这样：

while (queue.isEmpty()) {
    解锁后等待通知唤醒再加锁(用来收发通知的东西, lock);
}

生产者只需在往 queue 中 push 数据后这样，就可以完成协作：
```
    触发通知(用来收发通知的东西);
    // 一般有两种方式：
    //   通知所有在等待的（notifyAll / broadcast）
    //   通知
```
这就是条件变量（condition variable），也称作条件锁。它解决的问题不是`exclusively`，而是`waiting condition`。


## 读写锁（readers-writer lock）
在执行加锁操作时需要额外表明读写意图，readers之间并不互斥，而writer则要求与任何人互斥。
读写锁不需要特殊支持就可以直接用之前提到的几个东西实现，比如可以直接用两个 spinlock 或者两个 mutex 实现：
```
void 以读者身份加锁(rwlock) {
    加锁(rwlock.保护当前读者数量的锁);
    rwlock.当前读者数量 += 1;
    if (rwlock.当前读者数量 == 1) {
        加锁(rwlock.保护写操作的锁);
    }
    解锁(rwlock.保护当前读者数量的锁);
}

void 以读者身份解锁(rwlock) {
    加锁(rwlock.保护当前读者数量的锁);
    rwlock.当前读者数量 -= 1;
    if (rwlock.当前读者数量 == 0) {
        解锁(rwlock.保护写操作的锁);
    }
    解锁(rwlock.保护当前读者数量的锁);
}

void 以写者身份加锁(rwlock) {
    加锁(rwlock.保护写操作的锁);
}

void 以写者身份解锁(rwlock) {
    解锁(rwlock.保护写操作的锁);
}
```

