/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.edocx.trans.agentFX;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import org.json.simple.JSONObject;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.edocx.trans.EDocuments;
import org.rmj.edocx.trans.pojo.UnitEDocuments;
import org.rmj.edocx.trans.pojo.UnitEDocxDetail;
import org.rmj.parameters.agent.XMBranch;
import org.rmj.parameters.agent.XMDepartment;
import org.rmj.parameters.agent.XMEDocSysFile;
import org.rmj.client.agent.XMEmployee;
import org.rmj.parameters.agent.XMMCModel;

/**
 *
 * @author jef
 */
public class XMEDocuments {
    public XMEDocuments(GRider foGRider, String fsBranchCd, boolean fbWithParent){
        this.poGRider = foGRider;
        if(foGRider != null){
            this.pbWithParnt = fbWithParent;
            this.psBranchCd = fsBranchCd;
            poCtrl = new EDocuments();
            poCtrl.setGRider(foGRider);
            poCtrl.setBranch(psBranchCd);
            poCtrl.setOrigin(psOriginxx);
            poCtrl.setDepartment(psDeptIDxx);
            poCtrl.setEmployee(psEmployID);
            poCtrl.setEDocSyFile(psFileCode);
            
            poCtrl.setWithParent(true);
            pnEditMode = EditMode.UNKNOWN;
        }
    }

    public void setMaster(String fsCol, Object foData){
        setMaster(poData.getMaster().getColumn(fsCol), foData);
    }

    public void setMaster(int fnCol, Object foData) {
        if(pnEditMode != EditMode.UNKNOWN && poCtrl != null){
          // Don't allow update for sTransNox, cTranStat, sModified, and dModified
            if(!(fnCol == poData.getMaster().getColumn("sTransNox") ||
                fnCol == poData.getMaster().getColumn("cTranStat") ||
                fnCol == poData.getMaster().getColumn("sModified") ||
                fnCol == poData.getMaster().getColumn("dModified"))){

                if(fnCol == poData.getMaster().getColumn("dDateFrom") || 
                    fnCol == poData.getMaster().getColumn("dDateThru")){
                    if(foData instanceof Date)
                        poData.getMaster().setValue(fnCol, foData);
                    else
                    poData.getMaster().setValue(fnCol, null);
                }
                else{
                    poData.getMaster().setValue(fnCol, foData);
                }
            }
        }
    }

    public Object getMaster(String fsCol){
        return getMaster(poData.getMaster().getColumn(fsCol));
    }
   
    public Object getMaster(int fnCol) {
        if(pnEditMode == EditMode.UNKNOWN || poCtrl == null)
            return null;
        else{
            return poData.getMaster().getValue(fnCol);
        }
    }

    public Object getDetail(int row, String fsCol){
        return getDetail(row, poData.getDetail().get(row).getColumn(fsCol));
    }

    public Object getDetail(int row, int col){
        if(pnEditMode == EditMode.UNKNOWN || poCtrl == null)
            return null;
        else if(row < 0 || row >= poData.getDetail().size())
            return null;
      
        return poData.getDetail().get(row).getValue(col);
    }   

    public void setDetail(int row, String fsCol, Object value){
        setDetail(row, poData.getDetail().get(row).getColumn(fsCol), value);
    }

    public void setDetail(int row, int col, Object value){
        if(pnEditMode != EditMode.UNKNOWN && poCtrl != null){
            if(row >= 0 && row <= poData.getDetail().size()){
                if(row == poData.getDetail().size()){
                    poData.getDetail().add(new UnitEDocxDetail());
//                    poParts.add(null);
                }
             
                poData.getDetail().get(row).setValue(col, value);
            }
        }
    }   
   
    public void addDetail(){
        if(pnEditMode != EditMode.UNKNOWN && poCtrl != null){
            poData.getDetail().add(new UnitEDocxDetail());
//            poParts.add(null);
        }
    }
   
    public void deleteDetail(int row){
        if(pnEditMode != EditMode.UNKNOWN && poCtrl != null){
            if(!(row < 0 || row >= poData.getDetail().size()))
                poData.getDetail().remove(row);
        }
    }
   
    public boolean newTransaction() {
//        poParts.clear();

        if(poCtrl == null){
            return false;
        }

        poData = (UnitEDocuments) poCtrl.newTransaction();

        if(poData == null){
            return false;
        }
        else{
            //set the values of foreign key(object) to null
            poBranch = null;
            pnEditMode = EditMode.ADDNEW;
            return true;
        }
    }

    public boolean loadTransaction(String fsTransNox) {
//        poParts.clear();

        if(poCtrl == null){
            return false;
        }

        poData = (UnitEDocuments) poCtrl.loadTransaction(fsTransNox);

        if(poData.getMaster().getTransNox()== null){
            return false;
        }
        else{
            //set the values of foreign key(object) to null
            poBranch = null;
         
//            for(int lnCtr=0;lnCtr<=poData.getDetail().size();lnCtr++)
//                poParts.add(null);
         
            pnEditMode = EditMode.READY;
            return true;
        }
    }

    public boolean saveUpdate() {
        if(poCtrl == null){
            return false;
        }
        else if(pnEditMode == EditMode.UNKNOWN){
            return false;
        }
        else{
            if(!pbWithParnt) poGRider.beginTrans();
          
            UnitEDocuments loResult=null;
            if(pnEditMode == EditMode.ADDNEW)
                loResult = (UnitEDocuments) poCtrl.saveUpdate(poData, "");
            else
                loResult = (UnitEDocuments) poCtrl.saveUpdate(poData, (String) poData.getMaster().getValue(1));

            if(loResult == null){
                if(!pbWithParnt) poGRider.rollbackTrans();
                return false;
            }
            else{
                pnEditMode = EditMode.READY;
                poData = loResult;
                if(!pbWithParnt) poGRider.commitTrans();
                
                setMessage("Transaction saved successfully...");
                return true;
            }
        }
    }

    public boolean deleteTransaction(String fsTransNox) {
        if(poCtrl == null){
            return false;
        }
        else if(pnEditMode != EditMode.READY){
            return false;
        }
        else{
            if(!pbWithParnt) poGRider.beginTrans();
 
            boolean lbResult = poCtrl.deleteTransaction(fsTransNox);
            if(lbResult){
                pnEditMode = EditMode.UNKNOWN;
                if(!pbWithParnt) poGRider.commitTrans();
            }
            else
            if(!pbWithParnt) poGRider.rollbackTrans();

            return lbResult;
        }
    }

    public boolean closeTransaction(String fsTransNox) {
        if(poCtrl == null){
            return false;
        }
        else if(pnEditMode != EditMode.READY){
            setMessage("Edit mode does not allow verification of transaction!");         
            return false;
        }
        else{
            boolean lbResult = poCtrl.closeTransaction(fsTransNox);
            if(lbResult){
                setMessage("Transaction verified successfully!");
                pnEditMode = EditMode.UNKNOWN;
            }
            else
                setMessage(poCtrl.getErrMsg() + poCtrl.getMessage());

            return lbResult;
        }
    }

    public boolean postTransaction(String fsTransNox) {
        if(poCtrl == null){
            return false;
        }
        else if(pnEditMode != EditMode.READY){
            return false;
        }
        else{
            if(!pbWithParnt) poGRider.beginTrans();

            boolean lbResult = poCtrl.postTransaction(fsTransNox);
            if(lbResult){
                pnEditMode = EditMode.UNKNOWN;
                if(!pbWithParnt) poGRider.commitTrans();
            }
            else
                if(!pbWithParnt) poGRider.rollbackTrans();
             
            return lbResult;
        }
    }

    public boolean voidTransaction(String fsTransNox) {
        if(poCtrl == null){
            return false;
        }
        else if(pnEditMode != EditMode.READY){
            return false;
        }
        else{
            if(!pbWithParnt) poGRider.beginTrans();

            boolean lbResult = poCtrl.voidTransaction(fsTransNox);
            if(lbResult){
                pnEditMode = EditMode.UNKNOWN;
                if(!pbWithParnt) poGRider.commitTrans();
            }
            else
                if(!pbWithParnt) poGRider.rollbackTrans();
             
            return lbResult;
        }
    }

    public boolean cancelTransaction(String fsTransNox) {
        if(poCtrl == null){
            return false;
        }
        else if(pnEditMode != EditMode.READY){
            return false;
        }
        else{
            if(!pbWithParnt) poGRider.beginTrans();

            boolean lbResult = poCtrl.cancelTransaction(fsTransNox);
            if(lbResult){
                pnEditMode = EditMode.UNKNOWN;
                if(!pbWithParnt) poGRider.rollbackTrans();
            }
            else
                if(!pbWithParnt) poGRider.rollbackTrans();
             
            return lbResult;
        }
    }

//    public boolean searchWithCondition(String fsFieldNm, String fsValue, String fsFilter){
//        System.out.println("Inside SearchWithCondition");
//        fsValue = fsValue.trim();
//        
//        if(fsValue.trim().length() == 0){
//            setMessage("Nothing to process!");
//            return false;
//        }
//
//        String lsSQL = getSQL_Master();
//        if(fsFieldNm.equalsIgnoreCase("sTransNox")){
//            String lsPrefix = "";
//            if(fsValue.trim().length() <= 0 || fsValue.contains("%"))
//                lsPrefix = "";
//            else if(fsValue.length() <= 6)
//                lsPrefix = psBranchCd + SQLUtil.dateFormat(poGRider.getSysDate(), "yy");
//            else if(fsValue.length() <= 8)
//                lsPrefix = psBranchCd;
//            if(pnEditMode != EditMode.UNKNOWN){
//                if(fsValue.trim().equalsIgnoreCase((String)poData.getMaster().getValue(fsFieldNm))){
//                    setMessage("The same transaction code!");
//                    return false;
//                }
//            }
//            lsSQL = MiscUtil.addCondition(lsSQL, "sTransNox LIKE " + SQLUtil.toSQL(lsPrefix + "%" + fsValue));
//        }
//        else if(fsFieldNm.equalsIgnoreCase("sPromCode")){
//            String []laNames = MiscUtil.splitName(fsValue);
//            if(pnEditMode != EditMode.UNKNOWN){
//                if(fsValue.trim().equalsIgnoreCase((String)poData.getMaster().getValue(fsFieldNm))){
//                    setMessage("The same Promo Code detected!");
//                    return false;
//                }
//            }
//            lsSQL = MiscUtil.addCondition(lsSQL, "sPromCode LIKE " + SQLUtil.toSQL(fsValue + "%"));
//        }
//        else if(fsFieldNm.equalsIgnoreCase("sPromDesc")){
//            if(pnEditMode != EditMode.UNKNOWN){
//                if(fsValue.trim().equalsIgnoreCase((String)poData.getMaster().getValue(fsFieldNm))){
//                    setMessage("The same Promo Description detected!");
//                    return false;
//                }
//            }
//
//            lsSQL = MiscUtil.addCondition(lsSQL, "sPromDesc LIKE " + SQLUtil.toSQL(fsValue + "%"));
//        }
//
//        if(!fsFilter.isEmpty()){
//            lsSQL = MiscUtil.addCondition(lsSQL, fsFilter);
//        }
//
//        System.out.println(lsSQL);
//
//        //Create the connection object
//        Connection loCon = poGRider.getConnection();
//
//        System.out.println("After doCon");
//
//        if(loCon == null){
//            setMessage("Invalid connection!");
//            return false;
//        }
//
//        boolean lbHasRec = false;
//        Statement loStmt = null;
//        ResultSet loRS = null;
//
//        try {
//            System.out.println("Before Execute");
//
//            loStmt = loCon.createStatement();
//            loRS = loStmt.executeQuery(lsSQL);
//
//            if(!loRS.next())
//                setMessage("No Record Found!");
//            else{
//                
//                
//                        
//                JSONObject loValue = showFXDialog.jsonBrowse(poGRider, loRS, 
//                                                        "Trans. No»Description»Code»Promo From»Promo Thru", 
//                                                        "sTransNox»sPromDesc»sPromCode»dDateFrom»dDateThru", 
//                                                        "sTransNox»sPromDesc»sPromCode»dDateFrom»dDateThru", 1);
//              
//                if (loValue != null){
//                    lbHasRec = loadTransaction((String) loValue.get("sTransNox"));
//                }
//            }
//
//            System.out.println("After Execute");
//
//        } catch (SQLException ex) {
//            ex.printStackTrace();
//            setMessage(ex.getMessage());
//        }
//        finally{
//            MiscUtil.close(loRS);
//            MiscUtil.close(loStmt);
//        }
//
//        return lbHasRec;
//    }

    public boolean searchMaster(String field, String value){
        if(field.equalsIgnoreCase("sBranchCd")){
            return searchOrigin(field, value);
        }
        else if(field.equalsIgnoreCase("sBranchNm")){
            return searchOrigin(field, value);
        }
        else if(field.equalsIgnoreCase("sDeptIDxx")){
            return searchDepartment(field, value);
        }
        else if(field.equalsIgnoreCase("sDeptName")){
            return searchDepartment(field, value);
        }
        else if(field.equalsIgnoreCase("sEmployID")){
            return searchEmployee(field, value);
        }
        else if(field.equalsIgnoreCase("sCompnyNm")){
            return searchEmployee(field, value);
        }
        else if(field.equalsIgnoreCase("sFileCode")){
            return searchEDocSysFile(field, value);
        }
        else if(field.equalsIgnoreCase("sBarrCode")){
            return searchEDocSysFile(field, value);
        }
        else{
            setMessage("Invalid search field [" + field + "]  detected!");
            return false;
        }
    }
    
    public boolean searchWithCondition(String fsFieldNm, String fsValue, String fsFilter){
        System.out.println("Inside SearchWithCondition");
        fsValue = fsValue.trim();
        
        if(fsValue.trim().length() == 0){
            setMessage("Nothing to process!");
            return false;
        }//end: if(fsValue.trim().length() == 0)

        String lsSQL = getSQL_Master();
        if(fsFieldNm.equalsIgnoreCase("sTransNox")){
            String lsPrefix = "";
            if(fsValue.trim().length() <= 0 || fsValue.contains("%"))
                lsPrefix = "";
            else if(fsValue.length() <= 6)
                lsPrefix = psBranchCd + SQLUtil.dateFormat(poGRider.getSysDate(), "yy");
            else if(fsValue.length() <= 8)
                lsPrefix = psBranchCd;

            if(pnEditMode != EditMode.UNKNOWN){
                if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                    setMessage("The same transaction code!");
                    return false;
                }
            }//end: if(pnEditMode != EditMode.UNKNOWN)
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sTransNox LIKE " + SQLUtil.toSQL(lsPrefix + "%" + fsValue));
        }// end: if(fsFieldNm.equalsIgnoreCase("sTransNox"))
        else if(fsFieldNm.equalsIgnoreCase("sBranchNm")){
            if(pnEditMode != EditMode.UNKNOWN){
                if(fsValue.trim().equalsIgnoreCase((String)getMaster("sBranchNm"))){
                    setMessage("The same client name!");
                    return false;
                }
            }
                   
            lsSQL = MiscUtil.addCondition(lsSQL, "b.sBranchNm LIKE " + SQLUtil.toSQL(fsValue + "%"));
        }//end: if(fsFieldNm.equalsIgnoreCase("sTransNox")) - else if(fsFieldNm.equalsIgnoreCase("sClientNm"))=
        
        if(!fsFilter.isEmpty()){
           lsSQL = MiscUtil.addCondition(lsSQL, fsFilter);
        }

        System.out.println(lsSQL);

        //Create the connection object
        Connection loCon = poGRider.getConnection();

        if(loCon == null){
            setMessage("Invalid connection!");
            return false;
        }

        boolean lbHasRec = false;
        Statement loStmt = null;
        ResultSet loRS = null;

        try {
            System.out.println("Before Execute");

            loStmt = loCon.createStatement();
            loRS = loStmt.executeQuery(lsSQL);

            if(!loRS.next())
                setMessage("No Record Found!");
            else{
//                JSONObject loValue = showFXDialog.jsonBrowse(poGRider, loRS, 
//                                                                "Date»Branch»Department»Employee»Module", 
//                                                                "dTransact»sBranchNm»sDeptName»sCompnyNm»sModuleDs", 
//                                                                "a.dTransact»b.sBranchNm»c.sDeptName»d.sCompnyNm»e.sModuleDs", 1);
                
                JSONObject loValue = showFXDialog.jsonBrowse(poGRider, loRS, 
                                                                "Date»Branch»Department»Employee»Module", 
                                                                "a.dTransact»b.sBranchNm»c.sDeptName»d.sCompnyNm»e.sModuleDs");
              
                if (loValue != null){
                    lbHasRec = loadTransaction((String) loValue.get("sTransNox"));
                }
            }

            System.out.println("After Execute");

        }//end: try {
        catch (SQLException ex) {
            ex.printStackTrace();
            setMessage(ex.getMessage());
        }
        finally{
            MiscUtil.close(loRS);
            MiscUtil.close(loStmt);
            if(!pbWithParnt)
                MiscUtil.close(loCon);
        }

        return lbHasRec;
    }
    
    private boolean searchBranch(String fsFieldNm, String fsValue){
        System.out.println("Inside searchBranch");
        fsValue = fsValue.trim();
        
        if(fsValue.trim().length() == 0){
            setMessage("Nothing to process!");
            return false;
        }

        String lsSQL = getSQL_Branch();
        if(fsFieldNm.equalsIgnoreCase("sBranchNm")){
            if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                setMessage("The same Branch Name!");                
                return false;
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "sBranchNm LIKE " + SQLUtil.toSQL(fsValue + "%"));
        }
        else if(fsFieldNm.equalsIgnoreCase("sBranchCd"))
        {
            if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                setMessage("The same Branch Code!");
                return false;
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "sBranchCd LIKE " + SQLUtil.toSQL(fsValue));
        }
         
        //Create the connection object
        Connection loCon = poGRider.getConnection();

        if(loCon == null){
            setMessage("Invalid connection!");
            return false;
        }

        boolean lbHasRec = false;
        Statement loStmt = null;
        ResultSet loRS = null;

        try {
            System.out.println("Before Execute");

            loStmt = loCon.createStatement();
            System.out.println(lsSQL);
            loRS = loStmt.executeQuery(lsSQL);

            if(loRS.next()){
//                JSONObject loValue = showFXDialog.jsonBrowse(poGRider, loRS, 
//                                                                "Code»Description", 
//                                                                "sBranchCd»sBranchNm", 
//                                                                "sBranchCd»sBranchNm", 1);
                JSONObject loValue = showFXDialog.jsonBrowse(poGRider, loRS, 
                                                                "Code»Description", 
                                                                "sBranchCd»sBranchNm");
              
                if (loValue != null){
                    setBranch((String) loValue.get("sBranchCd"));
                    lbHasRec = true;
                }
            }

            System.out.println("After Execute");

        } catch (SQLException ex) {
            ex.printStackTrace();
            setMessage(ex.getMessage());
        }
        finally{
            MiscUtil.close(loRS);
            MiscUtil.close(loStmt);
        }

        return lbHasRec;
    }
    
    private boolean searchOrigin(String fsFieldNm, String fsValue){
        System.out.println("Inside searchOrigin");
        fsValue = fsValue.trim();
        
        if(fsValue.trim().length() == 0){
            setMessage("Nothing to process!");
            return false;
        }

        String lsSQL = getSQL_Branch();
        if(fsFieldNm.equalsIgnoreCase("sBranchNm")){
            if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                setMessage("The same Branch Name!");                
                return false;
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "sBranchNm LIKE " + SQLUtil.toSQL(fsValue + "%"));
        }
        else if(fsFieldNm.equalsIgnoreCase("sBranchCd"))
        {
            if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                setMessage("The same Branch Code!");
                return false;
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "sBranchCd LIKE " + SQLUtil.toSQL(fsValue));
        }
         
        //Create the connection object
        Connection loCon = poGRider.getConnection();

        if(loCon == null){
            setMessage("Invalid connection!");
            return false;
        }

        boolean lbHasRec = false;
        Statement loStmt = null;
        ResultSet loRS = null;

        try {
            System.out.println("Before Execute");

            loStmt = loCon.createStatement();
            System.out.println(lsSQL);
            loRS = loStmt.executeQuery(lsSQL);

            if(loRS.next()){
//                JSONObject loValue = showFXDialog.jsonBrowse(poGRider, loRS, 
//                                                                "Code»Description", 
//                                                                "sBranchCd»sBranchNm", 
//                                                                "sBranchCd»sBranchNm", 1);
                JSONObject loValue = showFXDialog.jsonBrowse(poGRider, loRS, 
                                                                "Code»Description", 
                                                                "sBranchCd»sBranchNm");
              
                if (loValue != null){
                    setOrigin((String) loValue.get("sBranchCd"));
                    lbHasRec = true;
                }
            }

            System.out.println("After Execute");

        } catch (SQLException ex) {
            ex.printStackTrace();
            setMessage(ex.getMessage());
        }
        finally{
            MiscUtil.close(loRS);
            MiscUtil.close(loStmt);
        }

        return lbHasRec;
    }
    
    private boolean searchDepartment(String fsFieldNm, String fsValue){
        System.out.println("Inside searchDeparment");
        fsValue = fsValue.trim();
        
        if(fsValue.trim().length() == 0){
            setMessage("Nothing to process!");
            return false;
        }

        String lsSQL = getSQL_Department();
        if(fsFieldNm.equalsIgnoreCase("sDeptName")){
            if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                setMessage("The same Department Name!");                
                return false;
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "sDeptName LIKE " + SQLUtil.toSQL(fsValue + "%"));
        }
        else if(fsFieldNm.equalsIgnoreCase("sDeptIDxx"))
        {
            if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                setMessage("The same Department Code!");
                return false;
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "sDeptIDxx LIKE " + SQLUtil.toSQL(fsValue));
        }

        //Create the connection object
        Connection loCon = poGRider.getConnection();

        if(loCon == null){
            setMessage("Invalid connection!");
            return false;
        }

        boolean lbHasRec = false;
        Statement loStmt = null;
        ResultSet loRS = null;

        try {
            System.out.println("Before Execute");

            loStmt = loCon.createStatement();
            System.out.println(lsSQL);
            loRS = loStmt.executeQuery(lsSQL);

            if(loRS.next()){
//                JSONObject loValue = showFXDialog.jsonBrowse(poGRider, loRS, 
//                                                                "Code»Department", 
//                                                                "sDeptIDxx»sDeptName", 
//                                                                "sDeptIDxx»sDeptName", 1);
                JSONObject loValue = showFXDialog.jsonBrowse(poGRider, loRS, 
                                                                "Code»Department", 
                                                                "sDeptIDxx»sDeptName");
              
                if (loValue != null){
                    setDepartment((String) loValue.get("sDeptIDxx"));
                    lbHasRec = true;
                }
            }

            System.out.println("After Execute");

        } catch (SQLException ex) {
            ex.printStackTrace();
            setMessage(ex.getMessage());
        }
        finally{
            MiscUtil.close(loRS);
            MiscUtil.close(loStmt);
        }

        return lbHasRec;
    }
   
    private boolean searchEmployee(String fsFieldNm, String fsValue){
        System.out.println("Inside searchEmployee");
        fsValue = fsValue.trim();
        
        if(fsValue.trim().length() == 0){
            setMessage("Nothing to process!");
            return false;
        }

        String lsSQL = getSQL_Employee();
        if(fsFieldNm.equalsIgnoreCase("sCompnyNm")){
            if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                setMessage("The same Empoyee Name!");                
                return false;
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "b.sCompnyNm LIKE " + SQLUtil.toSQL(fsValue + "%"));
        }
        else if(fsFieldNm.equalsIgnoreCase("sEmployID"))
        {
            if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                setMessage("The same Employee Code!");
                return false;
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sEmployID LIKE " + SQLUtil.toSQL(fsValue));
        }

        //Create the connection object
        Connection loCon = poGRider.getConnection();

        if(loCon == null){
            setMessage("Invalid connection!");
            return false;
        }

        boolean lbHasRec = false;
        Statement loStmt = null;
        ResultSet loRS = null;

        try {
            System.out.println("Before Execute");

            loStmt = loCon.createStatement();
            System.out.println(lsSQL);
            loRS = loStmt.executeQuery(lsSQL);

            if(loRS.next()){
//                JSONObject loValue = showFXDialog.jsonBrowse(poGRider, loRS, 
//                                                                "Code»Employee", 
//                                                                "sEmployID»sCompnyNm", 
//                                                                "a.sEmployID»b.sCompnyNm", 1);
                JSONObject loValue = showFXDialog.jsonBrowse(poGRider, loRS,    
                                                                "Code»Employee", 
                                                                "sEmployID»sCompnyNm");
              
                if (loValue != null){
                    setEmployee((String) loValue.get("sEmployID"));
                    lbHasRec = true;
                }
            }

            System.out.println("After Execute");

        } catch (SQLException ex) {
            ex.printStackTrace();
            setMessage(ex.getMessage());
        }
        finally{
            MiscUtil.close(loRS);
            MiscUtil.close(loStmt);
        }

        return lbHasRec;
    }
    
    private boolean searchEDocSysFile(String fsFieldNm, String fsValue){
        System.out.println("Inside searchModule");
        fsValue = fsValue.trim();
        
        if(fsValue.trim().length() == 0){
            setMessage("Nothing to process!");
            return false;
        }

        String lsSQL = getSQL_EDocSysFile();
        if(fsFieldNm.equalsIgnoreCase("sBarrCode")){
            if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                setMessage("The same Barcode!");                
                return false;
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "sBarrCode LIKE " + SQLUtil.toSQL(fsValue + "%"));
        }
        else if(fsFieldNm.equalsIgnoreCase("sFileCode"))
        {
            if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                setMessage("The same File Code!");
                return false;
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "sFileCode LIKE " + SQLUtil.toSQL(fsValue));
        }

        //Create the connection object
        Connection loCon = poGRider.getConnection();

        if(loCon == null){
            setMessage("Invalid connection!");
            return false;
        }

        boolean lbHasRec = false;
        Statement loStmt = null;
        ResultSet loRS = null;

        try {
            System.out.println("Before Execute");

            loStmt = loCon.createStatement();
            System.out.println(lsSQL);
            loRS = loStmt.executeQuery(lsSQL);

            if(loRS.next()){
//                JSONObject loValue = showFXDialog.jsonBrowse(poGRider, loRS, 
//                                                                "Code»Barcode»Description", 
//                                                                "sFileCode»sBarrCode»sBriefDsc", 
//                                                                "sFileCode»sBarrCode»sBriefDsc", 1);
                JSONObject loValue = showFXDialog.jsonBrowse(poGRider, loRS, 
                                                                "Code»Barcode»Description", 
                                                                "sFileCode»sBarrCode»sBriefDsc");
              
                if (loValue != null){
                    setEDocSysFile((String) loValue.get("sFileCode"));
                    lbHasRec = true;
                }
            }

            System.out.println("After Execute");

        } catch (SQLException ex) {
            ex.printStackTrace();
            setMessage(ex.getMessage());
        }
        finally{
            MiscUtil.close(loRS);
            MiscUtil.close(loStmt);
        }

        return lbHasRec;
    }
    
    public XMBranch getBranch(){
        if(poBranch == null)
            poBranch = new XMBranch(poGRider, psBranchCd, true);

        poBranch.openRecord(psBranchCd);
        return poBranch;
    }
     
    public XMBranch getOrigin(){
        if(poOrigin == null)
            poOrigin = new XMBranch(poGRider, psOriginxx, true);
        
        poOrigin.openRecord(psOriginxx);
        return poOrigin;
    }
    
    public XMDepartment getDepartment(){
        if(poDepartment == null)
            poDepartment = new XMDepartment(poGRider, psBranchCd, true);
        poDepartment.openRecord(psDeptIDxx);
        return poDepartment;
    }
    
    public XMEDocSysFile getEDocSysFile(){
        if(poEDocSysFile == null)
            poEDocSysFile = new XMEDocSysFile(poGRider, psFileCode, true);

        poEDocSysFile.openRecord(psFileCode);
        return poEDocSysFile;
    }
    
    public XMEmployee getEmployee(){
        if(poEmployee == null)
            poEmployee = new XMEmployee(poGRider, psEmployID, true);

        poEmployee.openRecord(psEmployID);
        return poEmployee;
    }

    public void setBranch(String fsBranchCD) {
        psBranchCd = fsBranchCD;
        poCtrl.setBranch(fsBranchCD);
    }
    
    public void setOrigin(String fsOriginxx) {
        psOriginxx = fsOriginxx;
        poCtrl.setOrigin(fsOriginxx);
    }
    
    public void setDepartment(String fsDeptIDxx) {
        psDeptIDxx = fsDeptIDxx;
        poCtrl.setDepartment(fsDeptIDxx);
    }
    
    public void setEmployee(String fsEmployID) {
        psEmployID = fsEmployID;
        poCtrl.setEmployee(fsEmployID);
    }
    
    public void setEDocSysFile(String fsFileCode) {
        psFileCode = fsFileCode;
        poCtrl.setEDocSyFile(fsFileCode);
    }

    public void setGRider(GRider foGRider) {
        this.poGRider = foGRider;
        poCtrl.setGRider(foGRider);
    }

    public int getEditMode() {
        return pnEditMode;
    }
   
    private String getSQL_Master(){
        return "SELECT" +
                    "  a.sTransNox" +
                    ", b.sBranchNm" +
                    ", c.sDepartNm" +
                    ", d.sCompnyNm" +
                    ", e.sModuleDs" +
                " FROM EDocSys_Master a" +
                    " LEFT JOIN Department c" +
                        " ON a.sDeptIDxx = c.sDeptIDxx" +
                    " LEFT JOIN Client_Master d" +
                        " ON a.sEmployID = d.sClientID" +
                    " LEFT JOIN EDocSys_Module e" +
                        " ON a.sModuleCd = e.sModuleCd" +
                    ", Branch b" +
                " WHERE a.sBranchCd = b.sBranchCd";
    }
    
    private String getSQL_Branch(){
        return "SELECT" +
                    "  sBranchCd" +
                    ", sBranchNm" +
                " FROM Branch" +
                " WHERE cRecdStat = '1'";
    }
    
    private String getSQL_Department(){
        return "SELECT" +
                    "  sDeptIDxx" +
                    ", sDeptName" +
                " FROM Department" +
                " WHERE cRecdStat = '1'";
    }

    private String getSQL_Employee(){
        return "SELECT" +
                    "  a.sEmployID" +
                    ", b.sCompnyNm" +
                " FROM Employee_Master001 a" +
                    ", Client_Master b" +
                " WHERE a.sEmployID = b.sClientID" +
                    " AND a.cRecdStat = '1'";
    }
    
    private String getSQL_EDocSysFile(){
        return "SELECT" +
                    "  sFileCode" +
                    ", sBarrCode" +
                    ", sBriefDsc" +
                " FROM EDocSys_File" +
                " WHERE cRecdStat = '1'";
    }
     
    public String getMessage(){return psMessage;}
    private void setMessage(String fsValue){psMessage = fsValue;}
   
//    ArrayList<XMGCardSpareparts> poParts = new ArrayList<XMGCardSpareparts>();
    private XMBranch poBranch = null;
    private XMBranch poOrigin = null;
    private XMDepartment poDepartment = null;
    private XMEDocSysFile poEDocSysFile = null;
    private XMEmployee poEmployee = null;

    private UnitEDocuments poData;
    private EDocuments poCtrl;
    private GRider poGRider;
    private int pnEditMode;
    private String psBranchCd;
    private String psOriginxx;
    private String psDeptIDxx;
    private String psEmployID;
    private String psFileCode;
    private String psMessage;
    private boolean pbWithParnt = false;
}
