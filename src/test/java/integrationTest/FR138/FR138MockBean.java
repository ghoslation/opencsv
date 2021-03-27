package integrationTest.FR138;

import com.opencsv.bean.processor.ConvertWordNullToNull;
import com.opencsv.bean.processor.PreAssignmentProcessor;

public class FR138MockBean {

    private String name;
    private String id;
    private String orderNumber;
    @PreAssignmentProcessor(processor = ConvertWordNullToNull.class)
    private int num;
    private double doubleNum;

    public FR138MockBean() {}

    public FR138MockBean(String name, String id, String orderNumber, int num, double doubleNum) {
        this.name = name;
        this.id = id;
        this.orderNumber = orderNumber;
        this.num = num;
        this.doubleNum = doubleNum;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public double getDoubleNum() {
        return doubleNum;
    }

    public void setDoubleNum(double doubleNum) {
        this.doubleNum = doubleNum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FR138MockBean)) return false;

        FR138MockBean mockBean = (FR138MockBean) o;

        if (getNum() != mockBean.getNum()) return false;
        if (Double.compare(mockBean.getDoubleNum(), getDoubleNum()) != 0) return false;
        if (getName() != null ? !getName().equals(mockBean.getName()) : mockBean.getName() != null) return false;
        if (getId() != null ? !getId().equals(mockBean.getId()) : mockBean.getId() != null) return false;
        return !(getOrderNumber() != null ? !getOrderNumber().equals(mockBean.getOrderNumber()) : mockBean.getOrderNumber() != null);

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = getName() != null ? getName().hashCode() : 0;
        result = 31 * result + (getId() != null ? getId().hashCode() : 0);
        result = 31 * result + (getOrderNumber() != null ? getOrderNumber().hashCode() : 0);
        result = 31 * result + getNum();
        temp = Double.doubleToLongBits(getDoubleNum());
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "FR138MockBean{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", orderNumber='" + orderNumber + '\'' +
                ", num=" + num +
                ", doubleNum=" + doubleNum +
                '}';
    }

}
