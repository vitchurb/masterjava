package ru.javaops.masterjava.service.mail.model;

import com.bertoncelj.jdbi.entitymapper.Column;
import lombok.*;
import ru.javaops.masterjava.persist.model.BaseEntity;
import ru.javaops.masterjava.service.mail.model.type.SendingResult;

import java.util.Date;

/**
 * Created by vit on 26.11.2017.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MailSendLogItem extends BaseEntity {
    @NonNull
    private Date dt;
    @Column("count_addresses")
    private int countAddresses;
    @NonNull
    private SendingResult result;
    @Column("result_comment")
    private String resultComment;
    @Column("addresses_to")
    private String addressesTo;
    @Column("addresses_cc")
    private String addressesCc;
}
