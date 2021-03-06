# POSIX Threads

## spinlock
```
  // 声明一个自旋锁变量
  pthread_spinlock_t spinlock;

  // 初始化   
  pthread_spin_init(&spinlock, 0);

  // 加锁  
  pthread_spin_lock(&spinlock);

  // 解锁 
  pthread_spin_unlock(&spinlock);

  // 销毁  
  pthread_spin_destroy(&spinlock);
```

## mutex（互斥量）
mutex（mutual exclusive）即互斥量（互斥体）。也便是常说的互斥锁。

尽管名称不含lock，但是称之为锁，也是没有太大问题的。mutex是最常见的多线程同步方式。其思想简单粗暴，多线程共享一个互斥量，然后线程之间去竞争。得到锁的线程可以进入临界区执行代码。
```
  // 声明一个互斥量    
  pthread_mutex_t mtx;
  // 初始化 
  pthread_mutex_init(&mtx, NULL);
  // 加锁  
  pthread_mutex_lock(&mtx);
  // 解锁 
  pthread_mutex_unlock(&mtx);
  // 销毁
  pthread_mutex_destroy(&mtx); 
```

mutex是睡眠等待（sleep waiting）类型的锁，当线程抢互斥锁失败的时候，线程会陷入休眠。优点就是节省CPU资源，缺点就是休眠唤醒会消耗一点时间。

自从Linux 2.6版以后，mutex完全用futex的API实现了，内部系统调用的开销大大减小。

值得一提的是，pthread的锁一般都有一个trylock的函数，比如对于互斥量：
```
ret = pthread_mutex_trylock(&mtx);
if (0 == ret) { // 加锁成功
    ... 
    pthread_mutex_unlock(&mtx);
} else if(EBUSY == ret){ // 锁正在被使用;
    ... 
}
```
pthread_mutex_trylock用于以非阻塞的模式来请求互斥量。就好比各种IO函数都有一个noblock的模式一样，对于加锁这件事也有类似的非阻塞模式。

当线程尝试加锁时，如果锁已经被其他线程锁定，该线程就会阻塞住，直到能成功acquire。但有时候我们不希望这样。pthread_mutex_trylock在被其他线程锁定时，会返回特殊错误码。加锁成返回0，仅当成功但时候，我们才能解锁在后面进行解锁操作！




## condition variable（条件变量）
条件变量不是锁，它是一种线程间的通讯机制，并且几乎总是和互斥量一起使用的。所以互斥量和条件变量二者一般是成套出现的。比如C++11中也有条件变量的API： std::condition_variable。


```
  // 声明一个互斥量     
  pthread_mutex_t mtx;
  // 声明一个条件变量
  pthread_cond_t cond;
  ...

  // 初始化 
  pthread_mutex_init(&mtx, NULL);
  pthread_cond_init(&cond, NULL);

  // 加锁  
  pthread_mutex_lock(&mtx);
  // 加锁成功，等待条件变量触发
  pthread_cond_wait(&cond, &mtx);

  ...
  // 加锁  
  pthread_mutex_lock(&mtx);
  pthread_cond_signal(&cond);
  ...

  // 解锁 
  pthread_mutex_unlock(&mtx);
  // 销毁
  pthread_mutex_destroy(&mtx);
```

## read-write lock（读写锁）
读写锁 对于临界区区分读和写。在读多写少的场景下，不加区分的使用互斥量显然是有点浪费的。

读写锁有一个别称叫 共享-独占锁 . 准确的含义是：是一种 读共享，写独占的锁。

读写锁的特性：

当读写锁被加了写锁时，其他线程对该锁加读锁或者写锁都会阻塞（不是失败）。
当读写锁被加了读锁时，其他线程对该锁加写锁会阻塞，加读锁会成功。
因而适用于多读少写的场景。
```
// 声明一个读写锁
pthread_rwlock_t rwlock;
...
// 在读之前加读锁
pthread_rwlock_rdlock(&rwlock);

... 共享资源的读操作

// 读完释放锁
pthread_rwlock_unlock(&rwlock);

// 在写之前加写锁
pthread_rwlock_wrlock(&rwlock); 

... 共享资源的写操作

// 写完释放锁
pthread_rwlock_unlock(&rwlock);

// 销毁读写锁
pthread_rwlock_destroy(&rwlock);
```
其实加读锁和加写锁这两个说法可能会造成误导，让人误以为是有两把锁，其实读写锁是一个锁。所谓加读锁和加写锁，准确的说法可能是 `给读写锁加读模式的锁定和加写模式的锁`』。

读写锁和互斥量一样也有trylock函数，也是以非阻塞地形式来请求锁，不会导致阻塞。

 pthread_rwlock_tryrdlock(&rwlock)
 pthread_rwlock_trywrlock(&rwlock)


C++11中有互斥量、条件变量但是并没有引入读写锁。而在C++17中出现了一种新锁：std::shared_mutex。用它可以模拟实现出读写锁。demo代码可以直接参考cppreference：

std::shared_mutex - cppreference.com
​
en.cppreference.com


另外多读少写的场景有些特殊场景，可以用特殊的数据结构减少锁使用：

多读单写的线性数据。用数组实现环形队列，避免vector等动态扩张的数据结构，写在结尾，由于单写因而可以不加锁；读在开头，由于多读（避免重复消费）所以需要加一下锁（互斥量就行）。
多读单写的KV。可以使用双缓冲（double buffer）的数据结构来实现。double buffer同名的概念比较多，这里指的是foreground 和 backgroud 两个buffer进行切换的『0 - 1切换』技术。比如实现动态加载（热加载）配置文件的时候。可能会在切换间隙加一个短暂的互斥量，但是基本可以认为是lock free的。
