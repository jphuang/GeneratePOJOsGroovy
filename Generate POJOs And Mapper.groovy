import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil
import java.util.Date
import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * @author: jphuang
 * @date: 2019/03/18
 * @description:用于自动生成实体类
 */
packageName = "com.huya.niko.gambling.model;"
typeMapping = [
        (~/(?i)int/)                         : "Integer",
        (~/(?i)bigint/)                      : "Long",
        (~/(?i)float|double|decimal|real/)   : "double",
        (~/(?i)datetime|timestamp|date|time/): "Date",
        (~/(?i)/)                            : "String"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
  SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}


def generate(table, dir) {
  def className = javaName(table.getName(), true)
  def fields = calcFields(table)
  packageName = getPackageName(dir)
  new File(dir, className + ".java").withPrintWriter("utf-8") { out -> generateDO(table, out, className, fields) }
  new File(dir, className + "Mapper.java").withPrintWriter("utf-8") { out -> generateMapper(out, className,table.comment) }

}

// 获取包所在文件夹路径
def getPackageName(dir) {
  return dir.toString().replaceAll("\\\\", ".").replaceAll("/", ".").replaceAll("^.*src(\\.main\\.java\\.)?", "")
}
// 生成通用实体
def generateDO(table, out, className, fields) {

  out.println "package ${packageName};"
  out.println ""
  out.println "import lombok.Data;"
  out.println "import lombok.experimental.Accessors;"
  out.println "import javax.persistence.Table;"
  out.println "import javax.persistence.GeneratedValue;"
  out.println "import javax.persistence.Id;"
  out.println "import javax.persistence.Column;"
  out.println "import java.util.Date;"
  out.println "import io.swagger.annotations.ApiModelProperty;"
  out.println ""
  out.println "/**"
  out.println " * @author jphuang"
  out.println " * @date " + String.format('yyyy/MM/dd HH:mm:ss', new Date())
  out.println " * @description " + table.comment + " DO"
  out.println " */"
  out.println ""
  out.println "@Data"
  out.println "@Accessors(chain = true)"
  out.println "@Table(name=\"" + table.getName() + "\")"
  out.println "public class ${className} {"
  out.println ""
  fields.each() {
    out.println "\t@ApiModelProperty(\"${it.comment}\")"
    if (it.annos != "") out.println "  ${it.annos}"
    if ("id".equals(it.name)) {
      out.println "\t@Id"
      out.println "\t@GeneratedValue(generator = \"JDBC\")"
    }
    out.println "\tprivate ${it.type} ${it.name};"
    out.println ""
  }
  out.println ""
  out.println "}"
}
// 生成通用mapper
def generateMapper(out, className,comment) {

  out.println "package ${packageName};"
  out.println ""
  out.println "import tk.mybatis.mapper.common.Mapper;"
  out.println "import ${packageName}.${className};"
  out.println ""
  out.println "/**"
  out.println " * @author jphuang"
  out.println " * @date " + String.format('yyyy/MM/dd HH:mm:ss', new Date())
  out.println " * @description " + comment +  " dao"
  out.println " */"
  out.println ""
  out.println "public interface ${className}Mapper extends Mapper<${className}> {\n}"
  out.println ""
}

// 获取数据库属性
def calcFields(table) {
  DasUtil.getColumns(table).reduce([]) { fields, col ->
    def commentStr = col.getComment()
    def spec = Case.LOWER.apply(col.getDataType().getSpecification())
    def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
    fields += [[
                       name   : javaName(col.getName(), false),
                       type   : typeStr,
                       annos  : "\t@Column(name = \"" + col.getName() + "\")",
                       comment: commentStr
               ]
    ]
  }
}

def javaName(str, capitalize) {
  def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str.replaceAll("tb_gambling",""))
          .collect { Case.LOWER.apply(it).capitalize() }
          .join("")
          .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
  capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}
