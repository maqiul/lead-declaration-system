# Flowable导入路径简化规范

## 1. 基本原则

为了提高代码可读性和减少冗余，应尽可能使用简化的导入路径。

## 2. 推荐的简化导入

### 2.1 Engine相关
```java
// 推荐 ✅
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.HistoryService;

// 不推荐 ❌
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.HistoryService;
```

### 2.2 Runtime相关
```java
// 推荐 ✅
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;

// 不推荐 ❌
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
```

### 2.3 Task相关
```java
// 推荐 ✅
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.delegate.DelegateTask;

// 不推荐 ❌
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.delegate.DelegateTask;
```

### 2.4 Delegate相关
```java
// 推荐 ✅
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.TaskListener;

// 不推荐 ❌
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.TaskListener;
```

## 3. 特殊情况处理

### 3.1 命名冲突
当项目中存在同名类时，应使用完整路径：

```java
// 项目实体类
import com.declaration.entity.ProcessInstance;

// Flowable类使用完整路径
org.flowable.engine.runtime.ProcessInstance flowableInstance = ...;
```

### 3.2 静态导入
对于常用的常量，可以考虑静态导入：
```java
import static org.flowable.engine.history.HistoryLevel.FULL;
```

## 4. 自动生成代码规范

### 4.1 IDE设置
在IDE中设置自动导入规则：
- IntelliJ IDEA: Settings → Editor → Code Style → Java → Imports
- Eclipse: Window → Preferences → Java → Code Style → Organize Imports

### 4.2 Maven插件
可以在构建过程中自动优化导入：
```xml
<plugin>
    <groupId>net.revelc.code</groupId>
    <artifactId>impsort-maven-plugin</artifactId>
    <version>1.9.0</version>
    <configuration>
        <groups>java.,javax.,org.,com.</groups>
        <staticGroups>java,*</staticGroups>
        <removeUnused>true</removeUnused>
    </configuration>
</plugin>
```

## 5. 检查清单

生成代码后应检查：
- [ ] 是否使用了不必要的完整路径
- [ ] 是否存在命名冲突需要特殊处理
- [ ] 导入语句是否按规范排序
- [ ] 是否移除了未使用的导入

## 6. 示例对比

### 优化前：
```java
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

public class Example {
    private RuntimeService runtimeService;
    private TaskService taskService;
    
    public void startProcess() {
        org.flowable.engine.runtime.ProcessInstance instance = 
            runtimeService.startProcessInstanceByKey("key");
        List<org.flowable.task.api.Task> tasks = 
            taskService.createTaskQuery().list();
    }
}
```

### 优化后：
```java
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

public class Example {
    private RuntimeService runtimeService;
    private TaskService taskService;
    
    public void startProcess() {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("key");
        List<Task> tasks = taskService.createTaskQuery().list();
    }
}
```

## 7. 注意事项

1. **向后兼容**：简化导入不应影响现有功能
2. **团队一致性**：整个团队应遵循相同的规范
3. **渐进式改进**：可以逐步优化现有代码
4. **自动化工具**：利用IDE和构建工具自动优化

---
*此规范适用于所有新开发的Flowable相关代码*