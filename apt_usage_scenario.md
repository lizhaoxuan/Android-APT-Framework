## Android编译时注解框架6-APT的优缺点与应用

### 概述

如果你已经读完了前面的5章博客，相信你已经对APT整体已经比较熟悉了，所以，APT真的很简单对嘛？

但就像我前面提到过的，**APT是一套非常强大的机制，它唯一的限制在于你天马行空的设计！** 

APT有着非常简单的技术实现，但其应用场景却着实有点尴尬。我一直期望可以探索出ButterKnife和EventBus以外的应用场景，却始终未能如愿。姑且把我目前总结的成果列举，共勉~

（**本系列所讲APT均泛指编译时注解+代码生成**，虽然运行时注解也属APT）

### APT优点

- 对代码进行标记，在编译时收集信息，并做处理。

- 生成一套独立代码，辅助代码运行

- 生成代码位置的可控性（可以在任意包位置生成代码），与原有代码的关联性更为紧密方便

- 更为可靠的自动代码生成

- 自动生成的代码可以最大程度的简单粗暴，在不必考虑编写效率的情况下提高运行效率


### APT缺点

- APT往往容易被误解可以实现代码插入，然而事实是并不可以

- APT可以自动生成代码，但在运行时却需要主动调用

- 与GreenDao不同，GreenDao代码生成于app目录下，可以在编写时调用并修改。APT代码生成于Build目录，只能在运行时通过接口等方式进行操作。这意味着生成的代码必须要有一套固定的模板


### APT容易被你忽视的点

一个非常容易被你误解的点：只有被注解标记了的类或方法等，才可以被处理或收集信息。或者这样说，想要收集一些信息，只能先用注解修饰它。

产生这样误解容易引起一个问题：你可能会觉得一个需要大量注解的框架体验不好而决定放弃。

事实是怎么样呢？想一下同源的运行时注解+反射。反射可以通过一个类名便获取一个类的所有信息（方法、属性、方法参数等等等）。编译时注解也是可以的。当你修饰一个类时，可以通过类的Element获得类的属性和方法的Element,通过属性的Element可以获得属性所属类的信息，通过方法的Element可以获得所属类和其参数的信息。

说白了，编译时注解你也完全可以当反射来理解。

APT的优缺点都非常明显，优点足够了，缺点也不致命，只是让你在设计你的框架，选择技术方案时注意就好了。那么基于上面列出的几点，几个通用的应用场景就可以被设想了~**一定要放大你的脑洞！！！**

### 应用场景-信息收集与统计


注解的主要作用就是用于标记，所以最基础的应用就是信息收集与统计。可能你还是有点懵懵懂懂，没关系，举例子嘛~

#### 编译时代码检查或统计

统计可能会一点奇怪：看看我这次写了多少个方法呀，多少个类呀。回头可以给BOSS说一下，以后KPI用方法数来计算？抱歉我的脑洞也就这样了，你再扩展一下~

	//示例代码  类统计
	@Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        Messager messager = processingEnv.getMessager();
        int size = env.getElementsAnnotatedWith(GetMsg.class).size();
        messager.printMessage(Diagnostic.Kind.NOTE,
                "Annotation class size = " + size);
    }

代码检查就比较靠谱一点了：类名是不是首字母大写的驼峰式啊？方法名有没有问题呀？常量是不是全大写啊？

这里你可能比较好奇，我怎么检查啊，难道要给每个类都加一个注解嘛？不不不，你看刚刚我们才讲了：【APT容易被你忽视的点】，只需要一个就够了~

（此处有点瞎扯淡了，一般代码检查都不会这么干）

	
#### 运行时数据收集与统计

通常来说，最容易想到的一个应用方向就是生成一个类似于字符串到类的对应Map结构。

手写代码容易出错，交给APT来实现便可以将错误率降到最低。

另外还有一个灵感来源于一个你不陌生的类：BuildConfig.

在BuildConfig中存放着一些静态属性，而这些静态属性值是Grandle编译时赋予的。可能这里你最常用的就是 BuildConfig.DEBUG了。

同理，APT也可以实现这样的功能。


### 应用场景-事件代理

此类应用场景的标志框架是ButterKnife。通过生成的代码代理实现View绑定。

	//示例代码
	@Override
	public void bind(final Finder finder, final T target, Object source) {
    	//定义了一个View对象引用，这个对象引用被重复使用了（这可是一个偷懒的写法哦~）
    	View view;

    	//暂时不管Finder是个什么东西，反正就是一种类似于findViewById的操作。
    	view = finder.findRequiredView(source, 2131558541, "field 'accountEdit'");

    	//target就是我们的ForgetActivity，为ForgetActivity中的accountEdit赋值
    	target.accountEdit = finder.castView(view, 2131558541, "field 'accountEdit'");

    	view = finder.findRequiredView(source, 2131558543, "field 'forgetBtn' and method 'forgetOnClick'");
    	target.forgetBtn = finder.castView(view, 2131558543, "field 'forgetBtn'");

    	//给view设置一个点击事件
    	view.setOnClickListener(
            new butterknife.internal.DebouncingOnClickListener() {
                @Override
                public void doClick(android.view.View p0) {

                    //forgetOnClick()就是我们在ForgetActivity中写得事件方法。
                   target.forgetOnClick();

                }
            });
	}
	

#### ButterKnife扩展

ButterKnife绑定View的同时，我们也可以附加一些操作。

一个典型案例就是 前面博客提到的OnceClick

在给View设置监听事件时，添加一些自定操作。

	view = finder.findViewById(source, 2131492945);
        if (view != null) {
            view.setOnClickListener(new View.OnClickListener() {
                long time = 0L;
                @Override
                public void onClick(View v) {
                    long temp = System.currentTimeMillis();
                    if (temp - time >= intervalTime) {
                        time = temp;
                        target.once();
                    }
                }
            });
        }


#### 其他属性（跨域）初始化\赋值

ButterKnife的核心便是View的初始化操作，View可以初始化，其他对象的初始化当然也不在话下。

举一个列子：

Intent不能传输过大的数据量，那么在跳转Activity时有这大数据量传输的需求怎么办? APT遍可以解决。其核心原理通ButterKnife相同。

ActivityA向ActivityB的代理类ProxyB赋值，ProxyB初始化ActivityB的属性。

### 应用场景-代理执行 or "代码插入"

虽然前面有说过APT并不能像Aspectj一样实现代码插入，但是可以使用某种变种方式实现，就是使用上怪怪的。

####代理执行

用注解修饰一系列方法，由APT来代理执行。

此部分参考[CakeRun](https://github.com/lizhaoxuan/CakeRun)

	public class CrashApplication extends Application {
	
		@Override
    	public void onCreate() {
        	super.onCreate();
        	//初始化APT框架，由Apt代理类来调用下列init方法，并在其中做些处理
        	//某种程度实现了代码插入
        	CakeRun.getInstance().applicationInit();
    	}
	
		@AppInit(tag = 1, canSkip = true)
    	protected void init1() {
        	Log.d("TAG", "init1()  将引起crash。非关键路径可以跳过");
        	String s = null;
        	Log.d("TAG", s);
    	}

    	@AsyncInit(tag = 2, packageName = {"com.lizhaoxuan.cakerundemo.Lib1", "other packageName"})
    	protected void init2() {
        	Log.d("TAG", "AsyncInit2() 引起Crash ,关键路径不可跳过");
        	Lib1.AsyncInit();
    	}

    	@AppInit(tag = 3)
    	protected void init3() {
        	Log.d("TAG", "init3() 未引起crash");
    	}

    	@AppInit(tag = 4)
    	protected void init4() {
        	Log.d("TAG", "init4() 未引起crash");
    	}
	}
	
APT生成的代理类按照一定次序依次执行修饰了注解的初始化方法，并且在其中增加了一些逻辑判断，来决定是否要执行这个方法。从而绕过发生Crash的类。

#### 代码插入AOP

使用APT实现AOP

因为APT限制，通过click事件做切面，是最简单的，就是我们上面讲的 OnceClick
	 
	@CrashClick(id = R.id.btn, target = HomeActivity)
	public void startActivity() {
  		...
        this.startActivity(intent);
    }
    
但对于普通方法，可能就需要这样调用

    protected void onCreate(Bundle savedInstanceState) {
    
    	//...
    	
    	//原本是这样调用方法的
        startHomeActivity();

        //使用了APT，需要在调用方法时插入一些逻辑，比如做AOP切面
        //就需要这样调用
        AptClient.doMethod(this,"startHomeActivity");

    }

    @DemoTest(method = "startHomeActivity")
    protected void startHomeActivity() {

    }


APT生成代码样式：

	public void doMethod(Object target){
		int temp;
		if(//一些逻辑条件){
			//执行前做一些操作，比如记录
			temp = 2; 
			//执行真正的方法
			target.startHomeActivity();
		}else{
			//这个方法有些问题不能执行
		}
	}


### 应用场景-反射优化

编译时注解与反射异曲同工，只不过反射是在运行时获取类信息，编译时注解是在编译时获取类信息。所以反射可以做到事情，APT也是可以做到的。

#### EventBus优化

EventBus效率的桎梏点在于需要通过反射遍历类中的Event接收方法，虽然做了缓存优化，但对效率的影响还是比较严重的。如果使用APT进行优化，EventBus最大的缺点就被解决了。

APT优化:

- 使用编译时注解标记Event接收方法。

- 通过APT+代码生成，生成对应代理类，并提取所有Event接收方法

- 每次注册不在需要在原本的类里寻找Event接收方法，而是直接注册代理类。


### 应用场景-让代码返璞归真

实际项目开发中，往往为了提高开发效率，会牺牲一点性能。最简单的例子就是运行时注解的大量使用。

运行时注解的大量使用减少了很多代码的编写，但谁都知道这是有性能损耗的。不过权衡利弊下，我们选择了妥协。

以ORM数据库框架为例。

细数目前Android主流的数据库框架：GreenDao、OrmLite、Active Android 。

OrmLite、Active Android均使用了运行时注解作为辅助从而实现了ORM。极大地简化了数据库操作，在使用上是非常轻松便捷的。但也因为使用运行时注解，用到了反射，导致了数据库操作性能的下降。

而作为数据库操作速度最快的GreenDao，它的原理是通过java工程替我们在项目中写了一套代码，一套返璞归真的数据库操作代码。没有反射的影响，采用最普通的方式操作数据库，它的速度是最快的！

但缺点是GreenDao的使用太奇葩了……导致初学GreenDao很痛苦。这是一个很致命的缺点。

**那么通过APT，则是一个很好的技术方案：CakeDao**

[https://github.com/lizhaoxuan/CakeDao](https://github.com/lizhaoxuan/CakeDao)

与GreenDao同理，自动生成最普通的数据库操作代码，从而提高数据库操作效率。但因为APT是在编译时自动进行的，所以他的学习成本是非常小的。


















	

	








