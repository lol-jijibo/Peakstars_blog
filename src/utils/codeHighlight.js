/**
 * VS Code Dark+ 风格语法高亮引擎
 * 基于正则 token 匹配，支持语言切换后重新高亮
 */

/* ===== VS Code Dark+ 配色方案 ===== */
const C = {
  keyword: '#569cd6',       // 蓝色 - 关键字 (if, else, for, return...)
  control: '#c586c0',       // 紫色 - 控制流 (import, export, from...)
  storage: '#569cd6',       // 蓝色 - 存储 (const, let, var, class...)
  type: '#4ec9b0',          // 青绿 - 类型名 (String, Number, Promise...)
  function: '#dcdcaa',      // 黄色 - 函数名
  string: '#ce9178',        // 橙红 - 字符串
  number: '#b5cea8',        // 浅绿 - 数字
  comment: '#6a9955',       // 绿色 - 注释
  operator: '#d4d4d4',      // 浅灰 - 运算符
  punctuation: '#d4d4d4',   // 浅灰 - 标点
  variable: '#9cdcfe',      // 亮蓝 - 变量
  constant: '#4fc1ff',      // 亮蓝 - 常量 (true, false, null...)
  decorator: '#dcdcaa',     // 黄色 - 装饰器/注解
  regex: '#d16969',         // 红色 - 正则
  tag: '#569cd6',           // 蓝色 - HTML 标签
  attrName: '#9cdcfe',      // 亮蓝 - 属性名
  attrValue: '#ce9178',     // 橙红 - 属性值
  property: '#9cdcfe',      // 亮蓝 - 属性/键
  selector: '#d7ba7d',      // 金黄 - CSS 选择器
  cssProp: '#9cdcfe',       // 亮蓝 - CSS 属性
  cssValue: '#ce9178',      // 橙红 - CSS 值
  cssUnit: '#b5cea8',       // 浅绿 - CSS 单位
  sqlKeyword: '#569cd6',    // 蓝色 - SQL 关键字
  yamlKey: '#9cdcfe',       // 亮蓝 - YAML 键
  markup: '#569cd6',        // 蓝色 - 标记符号
  plaintext: '#d4d4d4',     // 浅灰 - 纯文本
}

function esc(s) {
  return String(s || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function span(cls, text) {
  return `<span class="tok-${cls}">${text}</span>`
}

/* ===== Token 规则定义 ===== */

function jsRules() {
  return [
    ['comment', /\/\/[^\n]*/],
    ['comment', /\/\*[\s\S]*?\*\//],
    ['string',  /`(?:[^`\\]|\\.)*`/],
    ['string',  /"(?:[^"\\]|\\.)*"/],
    ['string',  /'(?:[^'\\]|\\.)*'/],
    ['regex',   /\/(?=[^/\n])(?:[^/\\\n]|\\.)+\/[gimsuy]*/],
    ['number',  /\b0[xX][\da-fA-F]+\b/],
    ['number',  /\b\d+\.?\d*(?:e[+-]?\d+)?\b/],
    ['control',  /\b(?:import|export|from|as|default|require)\b/],
    ['storage', /\b(?:const|let|var|function|class|extends|new|typeof|instanceof|in|of|delete|void|yield|async|await|static|get|set|super)\b/],
    ['keyword', /\b(?:if|else|for|while|do|switch|case|break|continue|return|throw|try|catch|finally|with|debugger)\b/],
    ['constant',/\b(?:true|false|null|undefined|NaN|Infinity)\b/],
    ['type',    /\b(?:Array|Object|String|Number|Boolean|Symbol|Map|Set|WeakMap|WeakSet|Promise|Date|RegExp|Error|TypeError|RangeError|SyntaxError|JSON|Math|console|window|document|parseInt|parseFloat|isNaN|isFinite|encodeURI|decodeURI|Proxy|Reflect|BigInt)\b/],
    ['decorator',/@[\w$]+/],
    ['function',/\b[a-zA-Z_$][\w$]*(?=\s*\()/],
    ['property',/(?<=\.)\s*[a-zA-Z_$][\w$]*/],
    ['variable',/\b[a-zA-Z_$][\w$]*\b/],
    ['operator',/[+\-*/%=<>!&|^~?:]+/],
    ['punctuation',/[{}[\]();,.]/],
  ]
}

function tsRules() {
  return [
    ['comment', /\/\/[^\n]*/],
    ['comment', /\/\*[\s\S]*?\*\//],
    ['string',  /`(?:[^`\\]|\\.)*`/],
    ['string',  /"(?:[^"\\]|\\.)*"/],
    ['string',  /'(?:[^'\\]|\\.)*'/],
    ['regex',   /\/(?=[^/\n])(?:[^/\\\n]|\\.)+\/[gimsuy]*/],
    ['number',  /\b0[xX][\da-fA-F]+\b/],
    ['number',  /\b\d+\.?\d*(?:e[+-]?\d+)?\b/],
    ['control',  /\b(?:import|export|from|as|default|require|declare|module|namespace|type|keyof|infer|readonly)\b/],
    ['storage', /\b(?:const|let|var|function|class|extends|implements|new|typeof|instanceof|in|of|delete|void|yield|async|await|static|get|set|super|abstract|enum|interface|public|private|protected|override)\b/],
    ['keyword', /\b(?:if|else|for|while|do|switch|case|break|continue|return|throw|try|catch|finally|with|debugger|satisfies)\b/],
    ['constant',/\b(?:true|false|null|undefined|NaN|Infinity)\b/],
    ['type',    /\b(?:string|number|boolean|void|any|never|unknown|object|symbol|bigint|Array|Record|Partial|Required|Readonly|Pick|Omit|Exclude|Extract|NonNullable|ReturnType|InstanceType|Parameters|ConstructorParameters|Promise|Date|RegExp|Error|Map|Set|WeakMap|WeakSet)\b/],
    ['decorator',/@[\w$]+/],
    ['function',/\b[a-zA-Z_$][\w$]*(?=\s*[<(])/],
    ['property',/(?<=\.)\s*[a-zA-Z_$][\w$]*/],
    ['variable',/\b[a-zA-Z_$][\w$]*\b/],
    ['operator',/[+\-*/%=<>!&|^~?:]+/],
    ['punctuation',/[{}[\]();,.]/],
  ]
}

function pythonRules() {
  return [
    ['comment', /#[^\n]*/],
    ['string',  /"""[\s\S]*?"""|'''[\s\S]*?'''/],
    ['string',  /f"(?:[^"\\]|\\.)*"|f'(?:[^'\\]|\\.)*'/],
    ['string',  /"(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*'/],
    ['number',  /\b0[xX][\da-fA-F]+\b/],
    ['number',  /\b\d+\.?\d*(?:e[+-]?\d+)?\b/],
    ['control',  /\b(?:import|from|as|with|lambda|global|nonlocal)\b/],
    ['storage', /\b(?:class|def|async|await|yield|return|del|pass|raise|try|except|finally)\b/],
    ['keyword', /\b(?:if|elif|else|for|while|break|continue|and|or|not|in|is)\b/],
    ['constant',/\b(?:True|False|None|Ellipsis)\b/],
    ['decorator',/@[\w$.]+/],
    ['type',    /\b(?:int|float|str|bool|list|dict|tuple|set|frozenset|bytes|bytearray|memoryview|range|enumerate|zip|map|filter|sorted|reversed|len|print|input|type|isinstance|issubclass|super|property|staticmethod|classmethod|object|Exception|ValueError|TypeError|KeyError|IndexError|AttributeError|RuntimeError|StopIteration|NotImplementedError|IOError|OSError|ArithmeticError|ZeroDivisionError|OverflowError|FileNotFoundError)\b/],
    ['function',/\b[a-zA-Z_]\w*(?=\s*\()/],
    ['property',/(?<=\.)\s*[a-zA-Z_]\w*/],
    ['variable',/\b[a-zA-Z_]\w*\b/],
    ['operator',/[+\-*/%=<>!&|^~@:]+/],
    ['punctuation',/[{}[\]();,.]/],
  ]
}

function javaRules() {
  return [
    ['comment', /\/\/[^\n]*/],
    ['comment', /\/\*[\s\S]*?\*\//],
    ['string',  /"(?:[^"\\]|\\.)*"/],
    ['string',  /'(?:[^'\\]|\\.)*'/],
    ['number',  /\b0[xX][\da-fA-F]+[Ll]?\b/],
    ['number',  /\b\d+\.?\d*(?:[eE][+-]?\d+)?[fFdDlL]?\b/],
    ['control',  /\b(?:import|package|throws|throw)\b/],
    ['storage', /\b(?:public|private|protected|static|final|abstract|native|synchronized|volatile|transient|strictfp|class|interface|extends|implements|enum|new|super|this|void|instanceof)\b/],
    ['keyword', /\b(?:if|else|for|while|do|switch|case|default|break|continue|return|try|catch|finally|assert)\b/],
    ['constant',/\b(?:true|false|null)\b/],
    ['type',    /\b(?:int|long|short|byte|char|float|double|boolean|String|Integer|Long|Short|Byte|Character|Float|Double|Boolean|Object|Class|Thread|Runnable|Exception|RuntimeException|NullPointerException|IllegalArgumentException|ArrayIndexOutOfBoundsException|ClassCastException|IOException|FileNotFoundException|StringBuilder|StringBuffer|ArrayList|HashMap|HashSet|LinkedList|TreeMap|TreeSet|LinkedHashMap|List|Map|Set|Queue|Deque|Optional|Stream|Collectors|System|Math|Arrays|Collections|Comparable|Comparator|Override|Deprecated|SuppressWarnings|FunctionalInterface)\b/],
    ['decorator',/@\w+/],
    ['function',/\b[a-zA-Z_$][\w$]*(?=\s*\()/],
    ['property',/(?<=\.)\s*[a-zA-Z_$][\w$]*/],
    ['variable',/\b[a-zA-Z_$][\w$]*\b/],
    ['operator',/[+\-*/%=<>!&|^~?:]+/],
    ['punctuation',/[{}[\]();,.]/],
  ]
}

function cRules() {
  return [
    ['comment', /\/\/[^\n]*/],
    ['comment', /\/\*[\s\S]*?\*\//],
    ['string',  /"(?:[^"\\]|\\.)*"/],
    ['string',  /'(?:[^'\\]|\\.)*'/],
    ['number',  /\b0[xX][\da-fA-F]+[uUlL]*\b/],
    ['number',  /\b\d+\.?\d*(?:[eE][+-]?\d+)?[fFlLuU]*\b/],
    ['control',  /\b(?:include|define|undef|ifdef|ifndef|endif|if|elif|else|pragma)\b/],
    ['storage', /\b(?:auto|break|case|char|const|continue|default|do|double|else|enum|extern|float|for|goto|if|int|long|register|return|short|signed|sizeof|static|struct|switch|typedef|union|unsigned|void|volatile|while|inline|restrict)\b/],
    ['constant',/\b(?:NULL|EOF|stdin|stdout|stderr|TRUE|FALSE)\b/],
    ['type',    /\b(?:size_t|ptrdiff_t|FILE|DIR|pid_t|time_t|clock_t|va_list|jmp_buf|sig_atomic_t|uint8_t|uint16_t|uint32_t|uint64_t|int8_t|int16_t|int32_t|int64_t)\b/],
    ['function',/\b[a-zA-Z_]\w*(?=\s*\()/],
    ['variable',/\b[a-zA-Z_]\w*\b/],
    ['operator',/[+\-*/%=<>!&|^~?:]+/],
    ['punctuation',/[{}[\]();,.]/],
    ['markup',  /#\w+/],
  ]
}

function cppRules() {
  return [
    ['comment', /\/\/[^\n]*/],
    ['comment', /\/\*[\s\S]*?\*\//],
    ['string',  /"(?:[^"\\]|\\.)*"/],
    ['string',  /'(?:[^'\\]|\\.)*'/],
    ['number',  /\b0[xX][\da-fA-F]+[uUlL]*\b/],
    ['number',  /\b\d+\.?\d*(?:[eE][+-]?\d+)?[fFlLuU]*\b/],
    ['control',  /\b(?:include|define|undef|ifdef|ifndef|endif|if|elif|else|pragma|using|namespace|template|typename)\b/],
    ['storage', /\b(?:auto|break|case|char|const|continue|default|do|double|else|enum|extern|float|for|goto|if|int|long|register|return|short|signed|sizeof|static|struct|switch|typedef|union|unsigned|void|volatile|while|inline|restrict|class|public|private|protected|virtual|override|final|friend|operator|new|delete|this|nullptr|dynamic_cast|static_cast|reinterpret_cast|const_cast|explicit|mutable|constexpr|decltype|noexcept|static_assert|thread_local|alignas|alignof)\b/],
    ['constant',/\b(?:NULL|nullptr|true|false|EOF|stdin|stdout|stderr)\b/],
    ['type',    /\b(?:size_t|ptrdiff_t|string|vector|map|set|unordered_map|unordered_set|list|deque|queue|stack|pair|tuple|array|bitset|optional|variant|any|unique_ptr|shared_ptr|weak_ptr|make_unique|make_shared|auto_ptr|iostream|ostream|istream|stringstream|fstream|cin|cout|cerr|clog|endl|std|bool|int8_t|int16_t|int32_t|int64_t|uint8_t|uint16_t|uint32_t|uint64_t)\b/],
    ['function',/\b[a-zA-Z_]\w*(?=\s*[<(])/],
    ['variable',/\b[a-zA-Z_]\w*\b/],
    ['operator',/[+\-*/%=<>!&|^~?:]+/],
    ['punctuation',/[{}[\]();,.]/],
    ['markup',  /#\w+/],
  ]
}

function csharpRules() {
  return [
    ['comment', /\/\/[^\n]*/],
    ['comment', /\/\*[\s\S]*?\*\//],
    ['string',  /@"(?:[^"]|"")*"/],
    ['string',  /"(?:[^"\\]|\\.)*"/],
    ['string',  /'(?:[^'\\]|\\.)*'/],
    ['number',  /\b0[xX][\da-fA-F]+[uUlLfFdDmM]?\b/],
    ['number',  /\b\d+\.?\d*(?:[eE][+-]?\d+)?[fFdDmMlLuU]?\b/],
    ['control',  /\b(?:using|namespace|is|as|in|nameof|sizeof|typeof|stackalloc|when|where|select|from|where|orderby|group|into|join|let|ascending|descending|on|equals|by|dynamic|var|value|get|set|add|remove|init|required|record|with|nint|nuint)\b/],
    ['storage', /\b(?:public|private|protected|internal|static|readonly|const|volatile|extern|unsafe|fixed|ref|out|params|abstract|sealed|virtual|override|new|this|base|class|struct|interface|enum|delegate|event|operator|implicit|explicit|async|await|yield|return|void|try|catch|finally|throw|throws)\b/],
    ['keyword', /\b(?:if|else|for|foreach|while|do|switch|case|break|continue|default|goto|checked|unchecked|lock|fixed)\b/],
    ['constant',/\b(?:true|false|null)\b/],
    ['type',    /\b(?:bool|byte|sbyte|char|short|ushort|int|uint|long|ulong|float|double|decimal|string|object|dynamic|void|Task|ValueTask|IEnumerable|IQueryable|ICollection|IList|IDictionary|List|Dictionary|HashSet|Queue|Stack|LinkedList|SortedDictionary|SortedList|ObservableCollection|Array|Exception|ArgumentException|ArgumentNullException|InvalidOperationException|NotSupportedException|NotImplementedException|NullReferenceException|IndexOutOfRangeException|KeyNotFoundException|TimeoutException|AggregateException|System|Console|Math|Convert|DateTime|DateTimeOffset|TimeSpan|Guid|StringBuilder|Stream|FileStream|MemoryStream|StreamReader|StreamWriter|JsonSerializer|HttpClient|CancellationToken|Action|Func|Predicate|EventArgs|IProgress|Progress|Nullable)\b/],
    ['decorator',/@\w+/],
    ['function',/\b[a-zA-Z_]\w*(?=\s*\()/],
    ['property',/(?<=\.)\s*[a-zA-Z_]\w*/],
    ['variable',/\b[a-zA-Z_]\w*\b/],
    ['operator',/[+\-*/%=<>!&|^~?:]+/],
    ['punctuation',/[{}[\]();,.]/],
  ]
}

function goRules() {
  return [
    ['comment', /\/\/[^\n]*/],
    ['comment', /\/\*[\s\S]*?\*\//],
    ['string',  /`[^`]*`/],
    ['string',  /"(?:[^"\\]|\\.)*"/],
    ['string',  /'(?:[^'\\]|\\.)*'/],
    ['number',  /\b0[xX][\da-fA-F]+\b/],
    ['number',  /\b\d+\.?\d*(?:e[+-]?\d+)?\b/],
    ['storage', /\b(?:package|import|func|return|defer|go|select|switch|case|default|if|else|for|range|break|continue|var|const|type|struct|interface|map|chan|fallthrough|goto)\b/],
    ['constant',/\b(?:true|false|nil|iota)\b/],
    ['type',    /\b(?:bool|byte|int|int8|int16|int32|int64|uint|uint8|uint16|uint32|uint64|float32|float64|complex64|complex128|string|rune|error|any|comparable|uintptr)\b/],
    ['type',    /\b(?:fmt|os|io|http|json|time|sync|context|errors|strconv|strings|bytes|bufio|log|path|filepath|regexp|template|net|url|reflect|unsafe|runtime|testing|append|cap|close|copy|delete|len|make|new|panic|recover|print|println|complex|real|imag)\b/],
    ['function',/\b[a-zA-Z_]\w*(?=\s*\()/],
    ['property',/(?<=\.)\s*[a-zA-Z_]\w*/],
    ['variable',/\b[a-zA-Z_]\w*\b/],
    ['operator',/[+\-*/%=<>!&|^~:]+/],
    ['punctuation', /[{}[\]();,.]/],
  ]
}

function rustRules() {
  return [
    ['comment', /\/\/[^\n]*/],
    ['comment', /\/\*[\s\S]*?\*\//],
    ['string',  /"(?:[^"\\]|\\.)*"/],
    ['string',  /b"(?:[^"\\]|\\.)*"/],
    ['string',  /r(#*)"(?:[^"]|"(?!\1))*"\1/],
    ['number',  /\b0[xX][\da-fA-F_]+(?:[iu]\d*)?\b/],
    ['number',  /\b0[oO][0-7_]+(?:[iu]\d*)?\b/],
    ['number',  /\b0[bB][01_]+(?:[iu]\d*)?\b/],
    ['number',  /\b\d[\d_]*\.?[\d_]*(?:[eE][+-]?[\d_]+)?(?:[iu]\d*|f\d*)?\b/],
    ['control',  /\b(?:use|mod|crate|self|super|in|as|ref|pub|where|dyn|impl|for|if|else|match|while|loop|break|continue|return|yield|await|move)\b/],
    ['storage', /\b(?:let|mut|const|static|type|struct|enum|trait|fn|unsafe|extern|async|trait|impl|fn|type|union)\b/],
    ['constant',/\b(?:true|false|Some|None|Ok|Err)\b/],
    ['type',    /\b(?:bool|char|u8|u16|u32|u64|u128|usize|i8|i16|i32|i64|i128|isize|f32|f64|str|String|Vec|Box|Rc|Arc|Option|Result|Cow|Cell|RefCell|HashMap|HashSet|BTreeMap|BTreeSet|VecDeque|LinkedList|BinaryHeap|Range|Iterator|IntoIterator|FromIterator|Clone|Copy|Debug|Display|Default|From|Into|TryFrom|TryInto|AsRef|AsMut|Borrow|ToOwned|Sized|Send|Sync|Unpin|Drop|Fn|FnMut|FnOnce|Read|Write|Seek|BufRead|Error|Formatter|Result|println|print|format|eprintln|eprint|panic|assert|assert_eq|assert_ne|todo|unimplemented|unreachable|todo)\b/],
    ['decorator',/#\[[\s\S]*?\]/],
    ['function',/\b[a-zA-Z_]\w*(?=\s*[<(])/],
    ['property',/(?<=\.)\s*[a-zA-Z_]\w*/],
    ['variable',/\b[a-zA-Z_]\w*\b/],
    ['operator',/[+\-*/%=<>!&|^~?:]+/],
    ['punctuation',/[{}[\]();,.]/],
    ['markup',  /\$\w+|\$\{[^}]*\}/],
  ]
}

function phpRules() {
  return [
    ['comment', /\/\/[^\n]*/],
    ['comment', /\/\*[\s\S]*?\*\//],
    ['comment', /#[^\n]*/],
    ['string',  /"(?:[^"\\]|\\.)*"/],
    ['string',  /'(?:[^'\\]|\\.)*'/],
    ['number',  /\b0[xX][\da-fA-F]+\b/],
    ['number',  /\b\d+\.?\d*(?:e[+-]?\d+)?\b/],
    ['control',  /\b(?:use|namespace|as|require|include|require_once|include_once)\b/],
    ['storage', /\b(?:abstract|and|array|break|callable|case|catch|class|clone|const|continue|declare|default|die|do|echo|else|elseif|empty|endfor|endforeach|endif|endswitch|endwhile|eval|exit|extends|final|finally|for|foreach|function|global|goto|if|implements|interface|instanceof|insteadof|new|or|print|private|protected|public|return|static|switch|throw|trait|try|unset|var|while|xor|yield|match|fn|readonly|enum)\b/],
    ['constant',/\b(?:true|false|null|TRUE|FALSE|NULL|PHP_EOL|PHP_INT_MAX|PHP_FLOAT_MAX|PHP_VERSION|PHP_OS|STDIN|STDOUT|STDERR)\b/],
    ['type',    /\b(?:int|float|bool|string|array|object|callable|iterable|void|mixed|never|self|static|parent|Closure|Generator|Exception|ErrorException|InvalidArgumentException|RuntimeException|LogicException|TypeError|ArgumentCountError|ArithmeticError|DivisionByZeroError)\b/],
    ['variable',/\$\w+/],
    ['function',/\b[a-zA-Z_]\w*(?=\s*\()/],
    ['property',/(?<=\.)\s*[a-zA-Z_]\w*/],
    ['operator',/[+\-*/%=<>!&|^~?:]+/],
    ['punctuation',/[{}[\]();,.]/],
    ['markup',  /<\?php|<\?=|\?>/],
  ]
}

function rubyRules() {
  return [
    ['comment', /#[^\n]*/],
    ['comment', /=begin[\s\S]*?=end/],
    ['string',  /"(?:[^"\\]|\\.)*"/],
    ['string',  /'(?:[^'\\]|\\.)*'/],
    ['string',  /`(?:[^`\\]|\\.)*`/],
    ['string',  /:[a-zA-Z_]\w*/],
    ['number',  /\b0[xX][\da-fA-F]+\b/],
    ['number',  /\b\d+\.?\d*(?:e[+-]?\d+)?\b/],
    ['control',  /\b(?:require|require_relative|include|extend|raise|fail|catch|throw|loop|yield|block_given|lambda|proc)\b/],
    ['storage', /\b(?:def|end|class|module|do|begin|rescue|ensure|if|elsif|else|unless|case|when|for|while|until|break|next|redo|retry|return|alias|undef|defined|super|self|attr_accessor|attr_reader|attr_writer|private|protected|public|initialize|new|method_missing)\b/],
    ['constant',/\b(?:true|false|nil|TRUE|FALSE|NIL)\b/],
    ['type',    /\b(?:Array|Hash|String|Integer|Float|Symbol|Regexp|Range|Enumerator|IO|File|Dir|Time|Date|DateTime|Exception|StandardError|RuntimeError|TypeError|ArgumentError|NoMethodError|NameError|IndexError|KeyError|Proc|Method|UnboundMethod|Binding|Thread|Mutex|Queue|Struct|OpenStruct|Set|SortedSet|Gem|Bundler|Rake|RSpec|Rails|ActiveRecord|ActiveSupport|Kernel|Object|BasicObject|Comparable|Enumerable|Comparable)\b/],
    ['decorator',/@\w+/],
    ['function',/\b[a-zA-Z_]\w*[?!]?(?=\s)/],
    ['property',/(?<=\.)\s*[a-zA-Z_]\w*[?!]?/],
    ['variable',/@@?\w+|\b[A-Z]\w*\b/],
    ['variable',/\b[a-zA-Z_]\w*[?!]?\b/],
    ['operator',/[+\-*/%=<>!&|^~?:]+/],
    ['punctuation',/[{}[\]();,.]/],
  ]
}

function swiftRules() {
  return [
    ['comment', /\/\/[^\n]*/],
    ['comment', /\/\*[\s\S]*?\*\//],
    ['string',  /"(?:[^"\\]|\\.)*"/],
    ['number',  /\b0[xX][\da-fA-F]+[fl]?\b/],
    ['number',  /\b\d+\.?\d*(?:e[+-]?\d+)?[fl]?\b/],
    ['control',  /\b(?:import|as|is|in|where|guard|fallthrough|available|typealias|precedencegroup|operator|subscript|convenience|required|designated|lazy|weak|unowned|mutating|nonmutating|override|dynamic|final|open|indirect|infix|prefix|postfix|associativity|none|left|right|OptionSet|StringProtocol|Codable|Decodable|Encodable|CustomStringConvertible|CustomDebugStringConvertible|Equatable|Hashable|Comparable|Identifiable|Sendable|async|await|try|throw|throws|rethrows|any|some|Self)\b/],
    ['storage', /\b(?:let|var|func|class|struct|enum|protocol|extension|init|deinit|get|set|willSet|didSet|type|for|while|repeat|if|else|switch|case|default|break|continue|return|do|catch|nil|true|false|self|super|associatedtype|private|fileprivate|internal|public|open|static|override|typealias|inout|defer)\b/],
    ['constant',/\b(?:true|false|nil)\b/],
    ['type',    /\b(?:Int|Int8|Int16|Int32|Int64|UInt|UInt8|UInt16|UInt32|UInt64|Float|Double|Float80|Float16|Bool|String|Character|Array|Dictionary|Set|Range|ClosedRange|Slice|Optional|Result|Any|AnyObject|Void|Never|UUID|URL|Date|Data|NSError|Error|Publisher|Subscriber|Subject|ObservableObject|Published|State|Binding|ObservedObject|EnvironmentObject|View|ContentView|Color|Image|Text|Button|VStack|HStack|ZStack|List|NavigationStack|NavigationLink|Sheet|Alert|Timer|NotificationCenter|UserDefaults|FileManager|Bundle|JSONDecoder|JSONEncoder|URLSession|print|debugPrint|fatalError|assert|assertionFailure|precondition|preconditionFailure)\b/],
    ['function',/\b[a-zA-Z_]\w*(?=\s*[<(])/],
    ['property',/(?<=\.)\s*[a-zA-Z_]\w*/],
    ['variable',/\b[a-zA-Z_]\w*\b/],
    ['operator',/[+\-*/%=<>!&|^~?.]+/],
    ['punctuation',/[{}[\]();,:]/],
  ]
}

function kotlinRules() {
  return [
    ['comment', /\/\/[^\n]*/],
    ['comment', /\/\*[\s\S]*?\*\//],
    ['string',  /"""[\s\S]*?"""/],
    ['string',  /"(?:[^"\\]|\\.)*"/],
    ['string',  /'(?:[^'\\]|\\.)*'/],
    ['number',  /\b0[xX][\da-fA-F]+[lLfFdD]?\b/],
    ['number',  /\b\d+\.?\d*(?:[eE][+-]?\d+)?[lLfFdD]?\b/],
    ['control',  /\b(?:import|package|as|by|constructor|crossinline|dynamic|file|infix|inline|inner|noinline|operator|reified|tailrec|suspend|typealias|value|where|sealed|data|override|open|abstract|final|lateinit|external|expect|actual|vararg|out|in)\b/],
    ['storage', /\b(?:val|var|fun|class|object|interface|enum|type|for|while|do|if|else|when|break|continue|return|throw|try|catch|finally|is|!is|as|as?|this|super|new|null|true|false|it|private|protected|internal|public|companion|init|get|set|field|property|receiver|param|setparam|delegate)\b/],
    ['constant',/\b(?:true|false|null|Unit|Nothing)\b/],
    ['type',    /\b(?:Boolean|Byte|Short|Int|Long|Float|Double|Char|String|Array|IntArray|LongArray|FloatArray|DoubleArray|ByteArray|ShortArray|CharArray|BooleanArray|List|MutableList|Set|MutableSet|Map|MutableMap|Collection|Iterable|Sequence|Iterator|Pair|Triple|Result|Option|Optional|Range|IntRange|LongRange|CharRange|Throwable|Exception|RuntimeException|IllegalArgumentException|IllegalStateException|NullPointerException|IndexOutOfBoundsException|ClassCastException|NumberFormatException|ArithmeticException|UnsupportedOperationException|ConcurrentModificationException|IOException|FileNotFoundException|println|print|readLine|also|apply|let|run|with|takeIf|takeUnless|toList|toSet|toMap|filter|map|flatMap|forEach|groupBy|sortedBy|associate|reduce|fold|count|find|first|last|single|any|all|none|isEmpty|isNotEmpty|contains)\b/],
    ['decorator',/@\w+/],
    ['function',/\b[a-zA-Z_]\w*(?=\s*[<(])/],
    ['property',/(?<=\.)\s*[a-zA-Z_]\w*/],
    ['variable',/\b[a-zA-Z_]\w*\b/],
    ['operator',/[+\-*/%=<>!&|^~?:]+/],
    ['punctuation',/[{}[\]();,.]/],
  ]
}

function sqlRules() {
  return [
    ['comment', /--[^\n]*/],
    ['comment', /\/\*[\s\S]*?\*\//],
    ['string',  /'(?:[^'\\]|'')*'/],
    ['string',  /"(?:[^"\\]|"")*"/],
    ['number',  /\b\d+\.?\d*\b/],
    ['sqlKeyword',/\b(?:SELECT|FROM|WHERE|INSERT|INTO|VALUES|UPDATE|SET|DELETE|CREATE|TABLE|ALTER|DROP|INDEX|VIEW|JOIN|INNER|LEFT|RIGHT|OUTER|FULL|CROSS|ON|AND|OR|NOT|IN|BETWEEN|LIKE|IS|NULL|AS|ORDER|BY|GROUP|HAVING|LIMIT|OFFSET|UNION|ALL|EXISTS|DISTINCT|CASE|WHEN|THEN|ELSE|END|BEGIN|COMMIT|ROLLBACK|TRANSACTION|GRANT|REVOKE|PRIMARY|KEY|FOREIGN|REFERENCES|CONSTRAINT|DEFAULT|CHECK|UNIQUE|AUTO_INCREMENT|INCREMENT|CASCADE|RESTRICT|IF|EXISTS|COUNT|SUM|AVG|MIN|MAX|CAST|CONVERT|COALESCE|NULLIF|ISNULL|CONCAT|TRIM|UPPER|LOWER|LENGTH|SUBSTRING|REPLACE|ROUND|FLOOR|CEIL|NOW|DATE|TIME|TIMESTAMP|VARCHAR|CHAR|INT|INTEGER|BIGINT|SMALLINT|TINYINT|FLOAT|DOUBLE|DECIMAL|NUMERIC|BOOLEAN|TEXT|BLOB|CLOB|DATE|TIME|DATETIME|SERIAL|BIGSERIAL)\b/i],
    ['function',/\b[a-zA-Z_]\w*(?=\s*\()/],
    ['variable',/[?]|:\w+|@\w+/],
    ['operator',/[<>=!<>]+/],
    ['punctuation',/[{}[\]();,.]/],
  ]
}

function htmlRules() {
  return [
    ['comment', /<!--[\s\S]*?-->/],
    ['string',  /"(?:[^"\\]|\\.)*"/],
    ['string',  /'(?:[^'\\]|\\.)*'/],
    ['tag',     /<\/?[a-zA-Z][\w-]*/],
    ['attrName',/\b[a-zA-Z-][\w-]*(?==)/],
    ['punctuation',/[<>/=]/],
    ['property',/\b[a-zA-Z-][\w-]*\b/],
    ['operator', /=/],
  ]
}

function cssRules() {
  return [
    ['comment', /\/\*[\s\S]*?\*\//],
    ['string',  /"(?:[^"\\]|\\.)*"/],
    ['string',  /'(?:[^'\\]|\\.)*'/],
    ['number',  /\b\d+\.?\d*(%|px|em|rem|vh|vw|vmin|vmax|fr|cm|mm|in|pt|pc|ch|ex|deg|rad|grad|turn|s|ms|Hz|kHz|dpi|dpcm|dppx)?\b/],
    ['selector',/[.#@][\w-]+/],
    ['selector',/:[\w-]+/],
    ['selector',/::[\w-]+/],
    ['cssProp', /\b[\w-]+(?=\s*:)/],
    ['cssValue',/#[\da-fA-F]{3,8}\b/],
    ['cssValue',/\b(?:auto|inherit|initial|unset|none|block|inline|inline-block|flex|grid|inline-flex|inline-grid|table|table-row|table-cell|absolute|relative|fixed|sticky|static|hidden|visible|scroll|solid|dashed|dotted|double|groove|ridge|inset|outset|normal|bold|bolder|lighter|italic|oblique|center|left|right|top|bottom|middle|baseline|sub|super|text-top|text-bottom|uppercase|lowercase|capitalize|nowrap|pre|pre-wrap|pre-line|cover|contain|fill|fit-content|min-content|max-content|ease|ease-in|ease-out|ease-in-out|linear|step-start|step-end|forwards|backwards|both|alternate|alternate-reverse|running|paused|transparent|currentColor|space-between|space-around|space-evenly|stretch|flex-start|flex-end|column|column-reverse|row|row-reverse|wrap|wrap-reverse|pointer|default|move|text|wait|help|crosshair|not-allowed|grab|zoom-in|sepia|saturate|grayscale|contrast|brightness|blur|hue-rotate|invert|drop-shadow)\b/],
    ['selector',/@(?:media|import|keyframes|font-face|supports|charset|page|layer|container)\b/],
    ['cssValue',/url\([^)]*\)/],
    ['function',/\b[a-zA-Z-][\w-]*(?=\s*\()/],
    ['punctuation',/[{}();:,]/],
    ['operator',/[>+~*]/],
  ]
}

function shellRules() {
  return [
    ['comment', /#[^\n]*/],
    ['string',  /"(?:[^"\\]|\\.)*"/],
    ['string',  /'(?:[^'\\]|\\.)*'/],
    ['string',  /`(?:[^`\\]|\\.)*`/],
    ['number',  /\b\d+\b/],
    ['control',  /\b(?:if|then|else|elif|fi|for|while|until|do|done|case|esac|in|function|select|time|coproc|return|exit|break|continue)\b/],
    ['storage', /\b(?:local|export|declare|readonly|typeset|unset|shift|source|alias|unalias|set|trap|read|readarray|mapfile|printf|echo|cd|pushd|popd|dirs|pwd|ls|mkdir|rmdir|rm|cp|mv|ln|chmod|chown|chgrp|touch|cat|head|tail|more|less|wc|sort|uniq|grep|sed|awk|find|xargs|tee|pipe|curl|wget|ssh|scp|rsync|tar|gzip|gunzip|bzip2|bunzip2|zip|unzip|make|cmake|npm|npx|yarn|pnpm|pip|conda|docker|kubectl|git|apt|yum|brew)\b/],
    ['constant',/\b(?:true|false|0|1)\b/],
    ['variable',/\$\{[^}]+\}|\$\w+|\$\([^)]+\)/],
    ['variable',/[A-Z_][A-Z_0-9]*=/],
    ['operator',/[|&;<>!]+/],
    ['punctuation',/[{}[\]();]/],
    ['markup',  /^#!.*/],
  ]
}

function yamlRules() {
  return [
    ['comment', /#[^\n]*/],
    ['string',  /"(?:[^"\\]|\\.)*"/],
    ['string',  /'(?:[^'\\]|\\.)*'/],
    ['number',  /\b\d+\.?\d*\b/],
    ['constant',/\b(?:true|false|null|yes|no|on|off|True|False|TRUE|FALSE|YES|NO|ON|OFF|Null|NULL|~)\b/],
    ['yamlKey', /[\w.-]+(?=\s*:)/],
    ['punctuation',/[:\-[|>{}]/],
  ]
}

function jsonRules() {
  return [
    ['string',  /"(?:[^"\\]|\\.)*"(?=\s*:)/],
    ['string',  /"(?:[^"\\]|\\.)*"/],
    ['number',  /\b-?\d+\.?\d*(?:[eE][+-]?\d+)?\b/],
    ['constant',/\b(?:true|false|null)\b/],
    ['punctuation',/[{}[\]:,]/],
  ]
}

function xmlRules() {
  return [
    ['comment', /<!--[\s\S]*?-->/],
    ['string',  /"(?:[^"\\]|\\.)*"/],
    ['string',  /'(?:[^'\\]|\\.)*'/],
    ['tag',     /<\/?[a-zA-Z][\w:-]*/],
    ['attrName',/\b[a-zA-Z:][\w:-]*(?==)/],
    ['markup',  /<\?[\s\S]*?\?>|<!\[CDATA\[[\s\S]*?\]\]>/],
    ['markup',  /xmlns[\w:.-]*/],
    ['punctuation',/[<>/=]/],
    ['property',/\b[a-zA-Z:][\w:-]*\b/],
    ['operator', /=/],
  ]
}

function markdownRules() {
  return [
    ['markup',  /^#{1,6}\s+.*/],
    ['markup',  /^[-*_]{3,}\s*$/],
    ['string',  /\*\*[^*]+\*\*/],
    ['string',  /\*[^*]+\*/],
    ['string',  /__[^_]+__/],
    ['string',  /_[^_]+_/],
    ['string',  /~~[^~]+~~/],
    ['string',  /`[^`]+`/],
    ['markup',  /\[[^\]]*\]\([^)]*\)/],
    ['markup',  /!\[[^\]]*\]\([^)]*\)/],
    ['markup',  /^>\s+.*/],
    ['markup',  /^[-*+]\s+/],
    ['markup',  /^\d+\.\s+/],
    ['markup',  /^---+/],
    ['comment', /<!--[\s\S]*?-->/],
    ['number',  /^\d+/],
    ['punctuation',/[[\]()]/],
  ]
}

/* ===== 规则映射 ===== */
const RULE_MAP = {
  JavaScript: jsRules,
  TypeScript: tsRules,
  Python: pythonRules,
  Java: javaRules,
  C: cRules,
  'C++': cppRules,
  'C#': csharpRules,
  Go: goRules,
  Rust: rustRules,
  PHP: phpRules,
  Ruby: rubyRules,
  Swift: swiftRules,
  Kotlin: kotlinRules,
  SQL: sqlRules,
  HTML: htmlRules,
  CSS: cssRules,
  Shell: shellRules,
  YAML: yamlRules,
  JSON: jsonRules,
  XML: xmlRules,
  Markdown: markdownRules,
  'Plain Text': () => [],
}

/* ===== 高亮引擎 ===== */

function tokenize(code, lang) {
  const rulesFn = RULE_MAP[lang]
  const rules = rulesFn ? rulesFn() : []

  if (!rules.length) {
    return [esc(code)]
  }

  const combined = rules.map((r, i) => `(?<t${i}>${r[1].source})`).join('|')
  const re = new RegExp(combined, 'gm')

  const result = []
  let last = 0

  for (const m of code.matchAll(re)) {
    if (m.index > last) {
      result.push(esc(code.slice(last, m.index)))
    }

    let matched = false
    for (let i = 0; i < rules.length; i++) {
      const group = m.groups?.[`t${i}`]
      if (group !== undefined) {
        result.push(span(rules[i][0], esc(m[0])))
        matched = true
        break
      }
    }

    if (!matched) {
      result.push(esc(m[0]))
    }

    last = m.index + m[0].length
  }

  if (last < code.length) {
    result.push(esc(code.slice(last)))
  }

  return result
}

/**
 * 对原始代码文本执行语法高亮，返回带 <span> 的 HTML
 * @param {string} code  原始代码文本
 * @param {string} lang  语言名称（如 'JavaScript'）
 * @returns {string}     高亮后的 HTML
 */
export function highlight(code, lang) {
  const tokens = tokenize(code, lang)
  return tokens.join('')
}

export { RULE_MAP }
