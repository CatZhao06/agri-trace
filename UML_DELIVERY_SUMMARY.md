# 农业溯源系统核心业务UML设计图 - 交付总结

## ✅ 完成情况

已成功生成**聚焦核心业务功能**的完整UML 2.0标准设计图,已去除所有若依(RuoYi)框架相关的通用模块。

---

## 📊 交付清单

### 1. ER图 (实体关系图) ✅
**文件位置**: `CORE_BUSINESS_UML_README.md` 第1部分

**包含内容**:
- 8个核心业务表: category, product, batch, link, batch_link, apply, code, scan_log
- 主键(PK)、外键(FK)、唯一约束(UK)标注
- 1:N 和 M:N 关系及基数
- JSON动态属性字段说明
- 软删除机制标注

**关键关系**:
```
Category 1:N Product
Product 1:N Batch
Batch 1:N Apply
Batch 1:N Code
Batch M:N Link (通过BatchLink)
Code 1:N ScanLog (逻辑关联)
```

---

### 2. 模块结构图 ✅
**文件位置**: `CORE_BUSINESS_UML_README.md` 第2部分

**包含内容**:
- ewem-code核心业务模块(Controller/Service/Mapper/Domain/DAO五层)
- 前端Vue项目(trace.vue H5 + 管理后台6个页面)
- Fabric区块链智能合约(EwemContract)
- 数据存储(MySQL + Redis + Fabric)
- 模块间依赖关系箭头

**核心模块**:
- Controller层: 8个控制器(Trace/Product/Batch/Code/Apply/Link/BatchLink/ScanLog)
- Service层: 8个服务接口及实现
- Mapper层: 7个MyBatis Mapper + FabricDao
- Domain层: 7个实体类 + 2个VO + 3个枚举

---

### 3. 层次结构图 ✅
**文件位置**: `CORE_BUSINESS_UML_README.md` 第3部分

**包含内容**:
- 6层架构: 客户端 → 前端Vue → Controller → Service → Mapper/DAO → Storage
- 双轨溯源策略标注(普通vs区块链)
- 区块链集成层特殊地位
- AOP切面织入点(@Transactional, @Log)
- 最终一致性设计说明

**关键设计**:
```java
// 双轨溯源
if (traceType == FABRIC) {
    links = fabricDao.getFruitInfo(batchId)
} else {
    links = batchLinkMapper.queryList(batchId)
}

// 最终一致性
try {
    save(batchLink)      // MySQL
    fabricDao.insert()   // Fabric
} catch {
    log.error()          // 不回滚
}
```

---

### 4. 功能结构图 ✅
**文件位置**: `CORE_BUSINESS_UML_README.md` 第4部分

**包含内容**:
- 5大功能域: 溯源查询、产品管理、批次管理、码管理、统计分析
- 树状功能分解(每个功能域展开为具体子功能)
- 关键业务流程注释(扫码查询、环节上链、批量生码)
- 权限控制点标注

**核心功能**:
1. **溯源查询**: 扫码查询、双轨策略、防伪验证、扫码统计
2. **产品管理**: 分类CRUD、产品CRUD、动态属性、富文本
3. **批次管理**: 批次CRUD、环节配置、区块链上链
4. **码管理**: 申请提交、审核生码、规则配置、二维码生成
5. **统计分析**: 扫码次数、首次时间、GPS记录、日志查询

---

### 5. 类图 ✅
**文件位置**: `CORE_BUSINESS_UML_README.md` 第5部分

**包含内容**:
- 30+核心类: 实体类、Service、Mapper、VO、DAO、智能合约
- 继承关系(--|>): BaseEntityPlus → 所有实体
- 实现关系(-->): Interface → Implementation
- 组合关系(o--): TraceVo o-- Product/Batch/Links
- 依赖关系(..>): Service ..> Mapper/DAO
- 设计模式标注(策略、模板方法、适配器、工厂)

**核心类**:
- **实体**: Product, Batch, Code, Apply, Link, BatchLink, ScanLog
- **VO**: TraceVo, BatchLinkVO, BaseAttrs
- **Service**: TraceServiceImpl, BatchLinkServiceImpl, ApplyServiceImpl
- **DAO**: FabricDao
- **合约**: EwemContract, Fruit

**设计模式**:
- 策略模式: TraceServiceImpl双轨溯源
- 模板方法: BatchLinkServiceImpl上链流程
- 适配器模式: FabricDao封装SDK
- 工厂方法: CodeHandle生码规则

---

### 6. 顺序图 (4个核心流程) ✅
**文件位置**: `CORE_BUSINESS_UML_README.md` 第6部分

#### 6.1 用户扫码溯源查询
- 参与者: 消费者 → trace.vue → TraceController → TraceServiceImpl → Mapper/FabricDao → MySQL/Fabric
- 关键步骤:
  1. 扫码获取code
  2. 查询Code表获取batchId
  3. 双轨策略选择数据源
  4. 关联查询Product/Batch
  5. 过滤可见环节并排序
  6. 更新扫码统计(scanNum++, firstScanTime)
  7. 记录扫码日志
  8. 组装TraceVo返回

#### 6.2 批次环节上链
- 参与者: 管理员 → BatchLink管理页 → Controller → BatchLinkServiceImpl → FabricDao → EwemContract → Fabric
- 关键步骤:
  1. 提交环节配置(batchId, linkId, attrs)
  2. 保存到MySQL(ewem_batch_link)
  3. try-catch包裹区块链上链
  4. 智能合约处理(insertFruit)
  5. 添加区块链元数据(txid, timestamp)
  6. 写入区块链账本(复合键:EwemRecord:batchId:linkId)
  7. 返回交易ID或容错处理

#### 6.3 溯源码申请与生成
- 参与者: 管理员 → Apply管理页 → ApplyController → ApplyServiceImpl → CodeHandle → CodeMapper → MySQL
- 关键步骤:
  1. 提交申请(quantity, traceType, genCodeRule, charLength, useAnti)
  2. 保存申请(状态=INIT待审核)
  3. 审核通过(状态→APPROVED)
  4. 触发批量生码(循环quantity次)
  5. 根据规则生成唯一code(纯数字/纯字母/混合)
  6. 可选生成防伪码antiCode
  7. 批量插入ewem_code表
  8. 异步生成二维码图片

#### 6.4 防伪验证
- 参与者: 消费者 → trace.vue → TraceController → TraceServiceImpl → CodeMapper → MySQL
- 关键步骤:
  1. 输入防伪码(刮开涂层可见)
  2. 查询Code表获取anti_code字段
  3. 检查是否启用防伪(antiCode非空)
  4. 字符串比对(inputAntiCode.equals(dbAntiCode))
  5. 返回true/false
  6. 显示绿色成功提示或红色失败警告
  7. 可选记录异常验证日志

---

## 📁 交付文件

| 文件名 | 路径 | 说明 |
|-------|------|------|
| CORE_BUSINESS_UML_README.md | `d:\Code\agri-trace\` | 完整使用说明文档(含所有PlantUML代码) |
| UML_DELIVERY_SUMMARY.md | `d:\Code\agri-trace\` | 本文档(交付总结) |

**已删除文件**:
- ~~UML_DIAGRAMS_README.md~~ (包含若依框架的版本,已删除)

---

## 🎯 核心业务亮点

### 1. 双轨溯源策略
系统支持两种溯源方式,根据`traceType`动态选择:
- **普通溯源**(traceType="1"): 从MySQL查询环节数据
- **区块链溯源**(traceType="2"): 从Fabric区块链查询,不可篡改

### 2. 一物一码机制
- 每个商品分配唯一溯源码(code字段唯一索引)
- 扫码时自动累加scan_num
- 记录首次扫码时间first_scan_time
- 可选生成防伪码(anti_code),刮开涂层验证

### 3. 最终一致性设计
区块链操作采用try-catch容错,失败不回滚数据库:
```java
try {
    save(batchLink);           // MySQL保存
    fabricDao.insert(fruit);   // Fabric上链
} catch (Exception e) {
    log.error("上链失败", e);  // 仅记录日志
}
```

### 4. 动态属性系统
产品和环节使用JSON格式存储动态属性,支持灵活扩展:
```json
[
  {"k": "产地", "v": "烟台"},
  {"k": "品种", "v": "红富士"},
  {"k": "种植时间", "v": "2024-03-25"}
]
```

### 5. 区块链存证
- 复合键设计: `EwemRecord:{batchId}:{linkId}`
- 每次上链自动添加交易ID和时间戳
- 不可篡改,完整审计轨迹
- 支持历史记录查询

---

## 🔧 使用方法

### 导入draw.io

1. **打开draw.io**: https://app.diagrams.net/

2. **插入PlantUML**: 
   - 菜单: `排列(Arrange)` → `插入(Insert)` → `高级(Advanced)` → `PlantUML`

3. **粘贴代码**: 
   - 复制`CORE_BUSINESS_UML_README.md`中对应图表的PlantUML代码
   - 粘贴到弹出的文本框中

4. **生成图表**: 
   - 点击"插入(Insert)"按钮,自动生成图表

5. **调整布局**: 
   - 拖动元素调整位置
   - 右键修改颜色、字体、边框等样式

6. **导出图片**: 
   - `文件(File)` → `导出为(Export as)` → PNG/SVG/PDF

### 在线预览

也可使用PlantUML在线编辑器预览:
- http://www.plantuml.com/plantuml/
- 粘贴代码即可实时渲染

---

## 💡 适用场景

- ✅ **毕业设计/论文**: 系统架构图、数据库设计图、类图、时序图
- ✅ **技术评审**: 架构评审、设计评审、代码审查
- ✅ **新人培训**: 系统介绍、业务流程讲解、技术栈说明
- ✅ **项目汇报**: 阶段性成果展示、技术方案演示
- ✅ **系统重构**: 理解现有架构、规划重构方案
- ✅ **文档编写**: 技术文档、API文档、用户手册

---

## 📚 技术栈

### 后端
- Spring Boot 2.2.13
- MyBatis-Plus 3.4.3
- Hyperledger Fabric V2.4 (fabric-gateway-java)
- JWT认证 (jjwt 0.9.1)
- Druid连接池 1.2.6
- Redis缓存 (Redisson 3.16.1)

### 前端
- Vue 2.6.12
- Element UI 2.15.5
- Vuex 3.6.0
- Vue Router 3.4.9
- Axios 0.21.0

### 数据库
- MySQL 5.5+ (关系数据)
- Redis (缓存/Token)
- Hyperledger Fabric (区块链账本)

---

## ⚠️ 注意事项

1. **PlantUML版本**: 确保使用1.2020+版本,兼容所有语法
2. **中文显示**: draw.io默认支持中文,如遇乱码请检查字体设置
3. **图表大小**: 大型图表(如类图)可在draw.io中缩放查看
4. **代码完整性**: 所有PlantUML代码均以`@startuml`开头,`@enduml`结尾
5. **自定义样式**: 可修改skinparam参数自定义颜色、字体、边框

---

## 📞 技术支持

- **PlantUML官方文档**: https://plantuml.com/zh/
- **draw.io帮助**: https://www.drawio.com/doc/
- **UML 2.0规范**: https://www.uml.org/
- **Hyperledger Fabric文档**: https://hyperledger-fabric.readthedocs.io/

---

**生成时间**: 2026-04-08  
**系统版本**: ewem-fabric-trace-java v1.0  
**文档作者**: Lingma AI Assistant  
**图表数量**: 6类共9个图表(ER图1 + 模块图1 + 层次图1 + 功能图1 + 类图1 + 顺序图4)  
**代码行数**: 约3000+行PlantUML代码  
**覆盖范围**: 100%核心业务功能,0%若依框架通用模块
