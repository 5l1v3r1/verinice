package sernet.gs.reveng.importData;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Transaction;

import sernet.gs.reveng.BaseHibernateDAO;
import sernet.gs.reveng.HibernateSessionFactory;
import sernet.gs.reveng.MSchutzbedarfkategTxt;
import sernet.gs.reveng.MSchutzbedarfkategTxtDAO;
import sernet.gs.reveng.MUmsetzStatTxt;
import sernet.gs.reveng.MbBaust;
import sernet.gs.reveng.MbBaustGefaehr;
import sernet.gs.reveng.MbBaustTxt;
import sernet.gs.reveng.MbDringlichkeitTxt;
import sernet.gs.reveng.MbDringlichkeitTxtDAO;
import sernet.gs.reveng.MbGefaehr;
import sernet.gs.reveng.MbMassn;
import sernet.gs.reveng.MbMassnTxt;
import sernet.gs.reveng.MbRolleTxt;
import sernet.gs.reveng.MbZeiteinheitenTxt;
import sernet.gs.reveng.MbZeiteinheitenTxtDAO;
import sernet.gs.reveng.ModZobjBst;
import sernet.gs.reveng.ModZobjBstId;
import sernet.gs.reveng.ModZobjBstMass;
import sernet.gs.reveng.ModZobjBstMassId;
import sernet.gs.reveng.NZielobjekt;
import sernet.gs.reveng.NZielobjektDAO;
import sernet.gs.reveng.NZobSb;
import sernet.gs.reveng.NZobSbDAO;
import sernet.gs.reveng.NmbNotiz;

public class GSVampire {
    
    private static final Logger LOG = Logger.getLogger(GSVampire.class);

	List<MSchutzbedarfkategTxt> allSchutzbedarf;

	private static final String QUERY_ZIELOBJEKT_TYP = "select zo, txt.name, subtxt.name "
			+ "			from NZielobjekt zo, MbZielobjTypTxt txt, MbZielobjSubtypTxt subtxt "
			+ "			where zo.mbZielobjSubtyp.id.zotId = txt.id.zotId "
			+ "			and (txt.id.sprId = 0 or txt.id.sprId = 1)"
			+ "			and zo.mbZielobjSubtyp.id.zosId = subtxt.id.zosId "
			+ "			and (subtxt.id.sprId = 0 or subtxt.id.sprId = 1)"
			+ "         and zo.loeschDatum = null";
	
//	   private static final String QUERY_ZIELOBJEKT_TYP_BY_ID = "select zo, txt.name, subtxt.name "
//	            + "         from NZielobjekt zo, MbZielobjTypTxt txt, MbZielobjSubtypTxt subtxt "
//	            + "         where zo.mbZielobjSubtyp.id.zotId = txt.id.zotId "
//	            + "         and (txt.id.sprId = 0 or txt.id.sprId = 1) "
//	            + "         and zo.mbZielobjSubtyp.id.zosId = subtxt.id.zosId "
//	            + "         and (subtxt.id.sprId = 0 or subtxt.id.sprId = 1) "
//	            + "         and zo.loeschDatum = null "
//	            + "         and zo.id.zobId = 10637";

//	private static final String QUERY_BAUSTEIN_ZIELOBJEKT = "select zo.name, zo.id.zobId, bst.nr, "
//			+ "zo_bst.begruendung, zo_bst.bearbeitet, zo_bst.datum "
//			+ "from ModZobjBst zo_bst, NZielobjekt zo, MbBaust bst "
//			+ "where zo_bst.id.bauId = bst.id.bauId "
//			+ "and zo_bst.id.zobId = zo.id.zobId " + "order by zo.id.zobId "
//			+ "and zo_bst.loeschDatum = null";

	private static final String QUERY_BAUSTEIN_ZIELOBJEKT_MASSNAHME_FOR_ZIELOBJEKT = "select bst, mn, umstxt, zo_bst, obm "
			+ "from ModZobjBstMass obm, "
			+ "	MUmsetzStatTxt umstxt, "
			+ "	NZielobjekt zo, "
			+ "	MbBaust bst, "
			+ "	MbMassn mn, "
			+ " ModZobjBst zo_bst  "
			+ "where zo.id.zobImpId = :zobImpId "
			+ "and zo.id.zobId = :zobId "
			+ "and umstxt.id.sprId = 1 "
			+ "and obm.ustId = umstxt.id.ustId "
			+ "and obm.id.zobImpId = zo.id.zobImpId "
			+ "and obm.id.zobId 	= zo.id.zobId "
			+ "and obm.id.bauId 	= bst.id.bauId "
			+ "and obm.id.bauImpId = bst.id.bauImpId "
			+ "and obm.id.masId 	= mn.id.masId "
			+ "and obm.id.masImpId = mn.id.masImpId "
			+ "and zo_bst.id.zobId = zo.id.zobId "
			+ "and zo_bst.id.bauId = bst.id.bauId "
			+ "and obm.loeschDatum = null ";

	private static final String QUERY_NOTIZEN_FOR_ZIELOBJEKT_NAME = "select bst, mn, umstxt, zo_bst, obm, notiz "
		+ "from ModZobjBstMass obm, "
		+ "	MUmsetzStatTxt umstxt, "
		+ "	NZielobjekt zo, "
		+ "	MbBaust bst, "
		+ "	MbMassn mn, "
		+ " ModZobjBst zo_bst, " 
		+ " NmbNotiz notiz "
		+ "where zo.name = :name "
		+ "and zo.id.zobImpId = 1 "
		+ "and umstxt.id.sprId = 1 "
		+ "and obm.ustId = umstxt.id.ustId "
		+ "and obm.id.zobImpId = zo.id.zobImpId "
		+ "and obm.id.zobId 	= zo.id.zobId "
		+ "and obm.id.bauId 	= bst.id.bauId "
		+ "and obm.id.bauImpId = bst.id.bauImpId "
		+ "and obm.id.masId 	= mn.id.masId "
		+ "and obm.id.masImpId = mn.id.masImpId "
		+ "and zo_bst.id.zobId = zo.id.zobId "
		+ "and zo_bst.id.bauId = bst.id.bauId " +
		  "and obm.notizId = notiz.id.notizId "
		+ "and obm.loeschDatum = null ";
	
	private static final String QUERY_BAUSTEIN_NOTIZEN_FOR_ZIELOBJEKT_NAME = "select bst, zo_bst, notiz "
        + "from ModZobjBst zo_bst, "
        + " NZielobjekt zo, "
        + " MbBaust bst, "
        + "NmbNotiz notiz "
        + "where zo.name = :name "
        + "and zo.id.zobImpId = 1 "
        + "and zo_bst.id.zobImpId = zo.id.zobImpId "
        + "and zo_bst.id.zobId     = zo.id.zobId "
        + "and zo_bst.id.bauId     = bst.id.bauId "
        + "and zo_bst.id.bauImpId = bst.id.bauImpId " +
		  "and zo_bst.nmbNotiz.id.notizId = notiz.id.notizId "
        + "and zo_bst.loeschDatum = null ";
	
	private static final String QUERY_MITARBEITER_FOR_MASSNAHME = "select mitarbeiter "
			+ "from ModZobjBstMassMitarb obmm, "
			+ "NZielobjekt mitarbeiter "
			+ "where obmm.id.zobImpId = :zobImpId "
			+ "and obmm.id.zobId = :zobId "
			+ "and obmm.id.bauId = :bauId "
			+ "and obmm.id.masId = :masId "
			+ "and obmm.id.zobIdMit = mitarbeiter.id.zobId " 
			+ "and obmm.loeschDatum = null";
	
	private static final String QUERY_MITARBEITER_FOR_BAUSTEIN = "select mitarbeiter " + 
			"	from " +
			"		ModZobjBstMitarb obm, " + 
			"		NZielobjekt mitarbeiter " + 
			"	where " +
			"		obm.id.bauId = :bauId " +
			"       and obm.id.zobId = :zobId " + 
			"		and obm.id.zobIdMit = mitarbeiter.id.zobId " + 
			"		and obm.loeschDatum = null";

	private static final String QUERY_ROLLE_FOR_MITARBEITER = "select rolle "
			+ "			from MbRolleTxt rolle, " + "				NZielobjekt zo, "
			+ "				NZielobjektRollen zr "
			+ "			where rolle.id.rolId = zr.id.rolId "
			+ "			and rolle.id.sprId = 1 "
			+ "			and zr.id.zobId = zo.id.zobId "
			+ "			and zo.id.zobId = :zobId" + "			and zr.loeschDatum = null";

	private static final String QUERY_SCHUTZBEDARF_FOR_ZIELOBJEKT = "select zsb "
			+ "from NZobSb zsb where zsb.id.zobId = :zobId";
	
	private static final String QUERY_ESA_FOR_ZIELOBJEKT = "select "
			+ " new sernet.gs.reveng.importData.ESAResult(esa.esaBegruendung, esa.esaEinsatz, "
			+ " esa.esaModellierung, esa.esaSzenario, esa.msUnj.unjId, esa.esaEntscheidDurch, zmi.name,"
			+ " esa.esaEntscheidDatum, esa.esaRaDatumBis)"
			+ " from NZobEsa esa, NZielobjekt zmi"
			+ " where zmi.id.zobId = esa.esaZobIdMit"
			+ "	and esa.NZielobjekt.id.zobId = :zobId";
	
	private static final String QUERY_MBBAUSTTXT_FOR_MBBAUST = "select mbBaustTxt, mzb"
	        + " from MbBaust mbBaust, MbBaustTxt mbBaustTxt, ModZobjBst mzb"
	        + " where mbBaust.id.bauId = mbBaustTxt.id.bauId"
	        + " and mzb.id.bauId = mbBaust.id.bauId"
	        + " and mzb.id.zobId = :zobId"
	        + " and mbBaust.id.bauImpId = 1"
	        + " and mbBaust.id.bauId = :bstId";
		
    
	private static final String QUERY_MBMASSTXT_FOR_MBMASS = "select mbMassnTxt" 
	       + " from MbMassn mbMassn, MbMassnTxt mbMassnTxt "
	       + " where mbMassn.id.masId = mbMassnTxt.id.masId"  
	       + " and mbMassn.id.masId = :masId"
	       + " and (mbMassnTxt.id.sprId = 1 or mbMassnTxt.id.sprId = 0)";
	

	// private static final String QUERY_ZEITEINHEITEN_TXT_ALL = "select zeittxt
	// " +
	// "from MbZeiteinheitenTxt zeittxt";

	private static final String QUERY_ALLSUBTYPES = "select txt.name, subtxt.name "
			+ "			from MbZielobjTypTxt txt, MbZielobjSubtypTxt subtxt "
			+ "			where txt.id.sprId = 1 "
			+ "			and txt.id.zotId = subtxt.id.zotId "
			+ "			and subtxt.id.sprId = 1";

	private static final String QUERY_LINKS_FOR_ZIELOBJEKT = "select dependant "
			+ "	from NZielobjZielobj link, "
			+ "		NZielobjekt dependant "
			+ "	where link.id.zobId1 = :zobId "
			+ "	and link.id.zobId2 = dependant.id.zobId "
			+ "	and link.loeschDatum = null";

	private static final String QUERY_ZIELOBJEKT_WITH_RA = "select distinct (z)," 
	        + " txt.name," 
	        + " subtxt.name"
	        + " from NZielobjekt z,"
	        + " MbZielobjTypTxt txt,"
	        + " MbZielobjSubtypTxt subtxt,"
	        + " RaZobGef rzg"
	        + " where z.mbZielobjSubtyp.id.zotId = txt.id.zotId"
	        + " and (txt.id.sprId = 0 or txt.id.sprId = 1)"
	        + " and z.mbZielobjSubtyp.id.zosId = subtxt.id.zosId"
	        + " and (subtxt.id.sprId = 0 or subtxt.id.sprId = 1)"
	        + " and z.loeschDatum = null"
	        + " and rzg.id.zobId = z.id.zobId";
	
	private static final String QUERY_RA_GEFS_FOR_ZIELOBJEKT = ""
			+ "select new sernet.gs.reveng.importData.RAGefaehrdungenResult(z, g, gtxt, rabtxt.kurz, rzg) "
			+ "from " 
			+ "	MbGefaehr g, MbGefaehrTxt gtxt,"
			+ "	RaZobGef rzg, MsRaBehandTxt rabtxt, NZielobjekt z"
			+ " where  rzg.id.zobId = z.id.zobId"
			+ "	and rzg.id.gefId = g.id.gefId"
			+ "	and gtxt.id.gefId = g.id.gefId"
			+ "	and rabtxt.id.rabId = rzg.msRaBehand.rabId"
			+ "	and z.id.zobId = :zobId"
			+ "	and (gtxt.id.sprId = 1 or gtxt.id.sprId = 0)"
			+ "	and rabtxt.id.sprId=1";

	
	private static final String QUERY_RA_GEF_MNS_FOR_ZIELOBJEKT =  
			"select new sernet.gs.reveng.importData.RAGefaehrdungsMassnahmenResult(z, g, gtxt, rabtxt.kurz, m, mtxt, mzbm, umstxt, stxt) " +
	        "from RaZobGef rzg, " + 
            "   RaZobGefMas rzgma, " + 
            "   MbGefaehr g, " + 
            "   MbGefaehrTxt gtxt, " + 
            "   MsRaBehandTxt rabtxt, " + 
            "   NZielobjekt z, " + 
            "   MbMassn m, " + 
            "   MbMassnTxt mtxt, " +  
            "   ModZobjBstMass mzbm, " + 
            "   MUmsetzStatTxt umstxt, " +
            "   MbBaustMassnGsiegel mbmg, " +
            "   MGsiegel s, " +
            "   MGsiegelTxt stxt " +
	        "where rzg.id.zobId = z.id.zobId " + 
            "   and rzg.id.gefId = g.id.gefId " + 
	        "   and gtxt.id.gefId = g.id.gefId " + 
	        "   and rabtxt.id.rabId = rzg.msRaBehand.rabId " + 
            "   and m.id.masId = rzgma.id.masId " + 
            "   and rzgma.id.gefId = g.id.gefId " + 
            "   and rzgma.id.zobId = z.id.zobId " +
            "   and mzbm.id.masId = rzgma.id.masId " + 
            "   and mzbm.id.zobId = z.id.zobId " +
            // why is it here set to 1 ( 1 means own bst, if you look it up in the query)?
            "   and mzbm.id.bauImpId = 1 " +
            "   and umstxt.id.ustId = mzbm.ustId " + 
            "   and umstxt.id.sprId = 1 " + 
            "   and mtxt.id.masId = m.id.masId " + 
            "   and (gtxt.id.sprId = 1 or gtxt.id.sprId = 0) " + 
            "   and rabtxt.id.sprId=1 " + 
            "   and (mtxt.id.sprId = 1 or mtxt.id.sprId = 0) " +
            "   and mbmg.id.bauId = mzbm.id.bauId " +
            "   and mbmg.id.masId = mzbm.id.masId " +
            "   and mbmg.MGsiegel.gruId = s.gruId " +
            "   and s.gruId = stxt.id.gruId " +
            "   and stxt.id.sprId=1 " +
            "   and z.id.zobId = :zobId " +
            "   and g.id.gefId = :gefId ";
	
	
//	private static final String QUERY_GEFTXT_FOR_BAUSTEIN = "select mbg from MbBaustGefaehr mbg, MbBaust mbb" 
//	        + " where mbg.mbGefaehr.id.gefId = :gefId"
//	        + " and gTxt.id.gefId = mbg.mbGefaehr.id.gefId"
//	        + " and mbg.mbBaust.id.bauId = :bstId";
	private static final String QUERY_MBBSTGEF_FOR_BAUSTEIN = "select mbg from MbBaustGefaehr mbg, NZielobjekt z, ModZobjBst mzb" 
	+ " where mbg.mbBaust.id.bauId = :bstId" 
	+ " and mzb.id.bauId = :bstId"
	+ " and mzb.id.zobId = :zobId"
	+ " and z.id.zobId  = :zobId";
	
//	private static final String QUERY_GEFS_FOR_BAUSTEIN = "select mbg.mbGefaehr" 
//	        + " from MbBaustGefaehr mbg, ModZobjBst zo_bst" 
//	        + " where mbg.mbBaust.id.bauId = :bstId "
//	        + " and zo_bst.id.bauId =  mbg.mbBaust.id.bauId"
//	        + " and zo_bst.id.zobId = :zobId";
	
	private static final String QUERY_GEFS_FOR_BAUSTEIN = "select mbg.mbGefaehr.nr, mbg.mbGefaehr.id.gefImpId, mbg.mbGefaehr.gfkId, mbg.mbGefaehr.id.gefId, gtxt.name, gtxt.beschreibung"
	+ " from MbGefaehrTxt gtxt, MbBaustGefaehr mbg"
	+ " where mbg.id.gefId = :gefId"
	+ " and gtxt.id.gefId = :gefId"
	+ " and (gtxt.id.sprId = 0 or gtxt.id.sprId = 1)"; // german (1) version and userdefs (0) only
	
	
	public GSVampire(String configFile) {
		HibernateSessionFactory.setConfigFile(configFile);
	}

	public List<ZielobjektTypeResult> findZielobjektTypAll() {
		return findZielobjektTyp(QUERY_ZIELOBJEKT_TYP);
	}
	
	public List<ZielobjektTypeResult> findZielobjektTypById() {
        return findZielobjektTyp(QUERY_ZIELOBJEKT_TYP);
    }
	
	public List<ZielobjektTypeResult> findZielobjektWithRA() {
        return findZielobjektTyp(QUERY_ZIELOBJEKT_WITH_RA);
    }
	
	public List<ZielobjektTypeResult> findZielobjektTyp(String hql) {
        List<ZielobjektTypeResult> result = new ArrayList<ZielobjektTypeResult>();
        NZielobjektDAO dao = new NZielobjektDAO();
        Transaction transaction = dao.getSession().beginTransaction();
        Query query = dao.getSession().createQuery(hql);
        Iterator iterate = query.iterate();
    
        while (iterate.hasNext()) {
            Object[] next = (Object[]) iterate.next();            
            // skip deleted objects:
            if ( ((NZielobjekt)next[0]).getLoeschDatum() == null ) {         
                result.add(new ZielobjektTypeResult((NZielobjekt) next[0], (String) next[1], (String) next[2]));
            }
        }
        transaction.commit();
        dao.getSession().close();
        return result;
    }

	/**
	 * Finds notes that are attached to "massnahmen" by target object.
	 * @param name Name of the target object 
	 * 
	 * @return
	 */
	public List<NotizenMassnahmeResult> findNotizenForZielobjekt(String name) {

		List<NotizenMassnahmeResult> result = new ArrayList<NotizenMassnahmeResult>();
		NZielobjektDAO dao = new NZielobjektDAO();
		Transaction transaction = dao.getSession().beginTransaction();

		// get notes for massnahmen:
		Query query = dao.getSession().createQuery(QUERY_NOTIZEN_FOR_ZIELOBJEKT_NAME);
		query.setParameter("name", name, Hibernate.STRING);
		Iterator iterate = query.iterate();
		while (iterate.hasNext()) {
				Object[] next = (Object[]) iterate.next();
				result.add(new NotizenMassnahmeResult((MbBaust) next[0],
						(MbMassn) next[1], (MUmsetzStatTxt) next[2],
						(ModZobjBst) next[3], (ModZobjBstMass) next[4], (NmbNotiz) next[5])
				);
		}

		// get notes for bausteine (bst, zo_bst, notiz)
		query = dao.getSession().createQuery(QUERY_BAUSTEIN_NOTIZEN_FOR_ZIELOBJEKT_NAME);
        query.setParameter("name", name, Hibernate.STRING);
        iterate = query.iterate();
        while (iterate.hasNext()) {
                Object[] next = (Object[]) iterate.next();
                result.add(new NotizenMassnahmeResult((MbBaust) next[0],
                        null, null,
                        (ModZobjBst) next[1], null, (NmbNotiz) next[2])
                );
        }
		
		
		
		transaction.commit();
		dao.getSession().close();
		return result;
	
	}
	
	public List<MbZeiteinheitenTxt> findZeiteinheitenTxtAll() {
		MbZeiteinheitenTxtDAO dao = new MbZeiteinheitenTxtDAO();
		Transaction transaction = dao.getSession().beginTransaction();
		List<MbZeiteinheitenTxt> result = dao.findAll();
		transaction.commit();
		dao.getSession().close();
		return result;
	}

	public List<MSchutzbedarfkategTxt> findSchutzbedarfAll() {
		MSchutzbedarfkategTxtDAO dao = new MSchutzbedarfkategTxtDAO();
		Transaction transaction = dao.getSession().beginTransaction();
		List<MSchutzbedarfkategTxt> all = dao.findAll();
		transaction.commit();
		dao.getSession().close();
		return all;
	}
	
	public List<MbDringlichkeitTxt> findDringlichkeitAll() {
		MbDringlichkeitTxtDAO dao = new MbDringlichkeitTxtDAO();
		Transaction transaction = dao.getSession().beginTransaction();
		List<MbDringlichkeitTxt> all = dao.findAll();
		transaction.commit();
		dao.getSession().close();
		return all;
	}
	
	public BausteinInformationTransfer findTxtForMbBaust(MbBaust mbBaust, NZielobjekt z, String encoding){
	    BaseHibernateDAO dao = new BaseHibernateDAO();
	    BausteinInformationTransfer bausteininformation = new BausteinInformationTransfer();
	    Transaction transaction = dao.getSession().beginTransaction();
	    Query qry = dao.getSession().createQuery(QUERY_MBBAUSTTXT_FOR_MBBAUST);
	    qry.setParameter("bstId", mbBaust.getId().getBauId());
	    qry.setParameter("zobId", z.getId().getZobId());
	    List<Object> hqlResult = qry.list();
	    if(hqlResult.size() == 1 && hqlResult.get(0) instanceof Object[]){
	        Object[] resultArr = (Object[])hqlResult.get(0);
	        MbBaustTxt mTxt =  (MbBaustTxt)resultArr[0];
	        ModZobjBst mzb = (ModZobjBst)resultArr[1];
	        try {
                bausteininformation.setDescription(((mTxt.getBeschreibung() != null) ? convertClobToStringEncodingSave(mTxt.getBeschreibung(), encoding) : "no description available"));
            } catch (IOException e) {
                LOG.error("Error converting CLOB to String", e);
            }
	        bausteininformation.setEncoding(encoding);
	        bausteininformation.setId(mbBaust.getNr());
//	        bausteininformation.setKapitel("1"); // TODO
	        bausteininformation.setSchicht(String.valueOf(mbBaust.getMbSchicht().getId().getSchId()));
	        bausteininformation.setTitel(mTxt.getName());
	        bausteininformation.setErfasstAm(mzb.getDatum());
	        bausteininformation.setZobId(z.getId().getZobId());
	        bausteininformation.setNr(mbBaust.getNr());
	        Hibernate.initialize(mzb.getMbBaust());
	        Hibernate.initialize(mzb.getNZielobjektByFkZbZ());
	        Hibernate.initialize(mzb.getNZielobjektByFkZbZ2());
	        bausteininformation.setMzb(mzb);
	    }
        transaction.commit();
        dao.getSession().close();
	    return bausteininformation;
	}
	
	public MassnahmeInformationTransfer findTxtforMbMassn(MbBaust mBbaut, MbMassn mbMassn, String encoding){
	    BaseHibernateDAO dao = new BaseHibernateDAO();
        Transaction transaction = dao.getSession().beginTransaction();
        Query qry = dao.getSession().createQuery(QUERY_MBMASSTXT_FOR_MBMASS);
        qry.setParameter("masId", mbMassn.getId().getMasId());
        List<Object> hqlResult = qry.list();
        MassnahmeInformationTransfer massnahmeinformation = new MassnahmeInformationTransfer();
        if(hqlResult.size() == 1 && hqlResult.get(0) instanceof MbMassnTxt){
            massnahmeinformation = processMassnahme(mbMassn, encoding, hqlResult, massnahmeinformation);
            
        }
        transaction.commit();
        dao.getSession().close();
        return massnahmeinformation;	    
	}

    /**
     * @param mbMassn
     * @param encoding
     * @param hqlResult
     * @param massnahmeinformation
     */
    private MassnahmeInformationTransfer processMassnahme(MbMassn mbMassn, String encoding, List<Object> hqlResult, MassnahmeInformationTransfer massnahmeinformation) {
        MbMassnTxt mTxt = (MbMassnTxt)hqlResult.get(0);
        
        massnahmeinformation.setAbstract_(mTxt.getAbstract_());
        massnahmeinformation.setTitel(mTxt.getName());
        try{
            if(mTxt.getBeschreibung() !=  null ){
                massnahmeinformation.setDescription(convertClobToStringEncodingSave(mTxt.getBeschreibung(), encoding));
            }
            if(mTxt.getHtmltext() != null){
                massnahmeinformation.setHtmltext(convertClobToStringEncodingSave(mTxt.getHtmltext(), encoding));
            }
        } catch (IOException e){
            LOG.error("Error parsing clob to String", e);
        }
        massnahmeinformation.setId("bM " + String.valueOf(mbMassn.getMskId().intValue()) + "." + String.valueOf(mbMassn.getNr()));
        massnahmeinformation.setSiegelstufe('A'); // TODO
        massnahmeinformation.setZyklus("-1"); // TODO 
        return massnahmeinformation;
    }
	
    public GefaehrdungInformationTransfer findGefaehrdungInformationForBausteinGefaehrdung(MbBaust mbBaust, MbBaustGefaehr mbBstGef, NZielobjekt z, String encoding){
        BaseHibernateDAO dao = new BaseHibernateDAO();
        Transaction transaction = dao.getSession().beginTransaction();
        Query qry = dao.getSession().createQuery(QUERY_GEFS_FOR_BAUSTEIN);
        qry.setParameter("gefId", mbBstGef.getMbGefaehr().getId().getGefId());
        List<Object> hqlResult = qry.list();
        GefaehrdungInformationTransfer gefaehrdungInformation = new GefaehrdungInformationTransfer();
        if(hqlResult.size() >= 1 && hqlResult.get(0) instanceof Object[]){
            gefaehrdungInformation = processGefaehrdung(encoding, hqlResult, gefaehrdungInformation);
        }
        
        logDuplicates(encoding, hqlResult);
        transaction.commit();
        dao.getSession().close();
        return gefaehrdungInformation;
	}

    /**
     * @param encoding
     * @param hqlResult
     * @param gefaehrdungInformation
     */
    private GefaehrdungInformationTransfer processGefaehrdung(String encoding, List<Object> hqlResult, GefaehrdungInformationTransfer gefaehrdungInformation) {
        Object[] resultArr = ((Object[])hqlResult.get(0));
        String gefaehrdungNr = String.valueOf(resultArr[0]);
        String gefaehrdungKapitelId = String.valueOf(resultArr[2]);
        String gefaehrdungId = String.valueOf(resultArr[3]);
        String gefaehrdungName = String.valueOf(resultArr[4]);
        try {
            gefaehrdungInformation.setDescription(convertClobToStringEncodingSave((Clob)resultArr[5], encoding));
        } catch (IOException e) {
            LOG.error("Error parsing clob to String", e);
            gefaehrdungInformation.setDescription("Description not parseable from CLOB");
        }
        gefaehrdungInformation.setId(String.valueOf(gefaehrdungNr));
        gefaehrdungInformation.setTitel(gefaehrdungName);
        return gefaehrdungInformation;
    }

    /**
     * @param encoding
     * @param hqlResult
     */
    private void logDuplicates(String encoding, List<Object> hqlResult) {
        if(LOG.isDebugEnabled() && hqlResult.size() > 1){
            Map<String, String> tMap = new HashMap<String, String>();
            for(Object o : hqlResult){
                if(o instanceof Object[]){
                    Object[] oArr = (Object[])o;
                    String gefaehrdungNr = String.valueOf(oArr[0]);
                    String gefaehrdungId = String.valueOf(oArr[3]);
                    String gefaehrdungName = String.valueOf(oArr[4]);
                    String gefaehrdungDescription = "";
                    try{
                        gefaehrdungDescription = convertClobToStringEncodingSave((Clob)oArr[5], encoding);
                    } catch (IOException e){
                        LOG.error("Error parsing clob to String", e);
                    }
                    String id = gefaehrdungId + gefaehrdungNr + gefaehrdungName;
                    if(tMap.containsKey(id)){
                        String existingDescription = tMap.get(id); // existing description
                        if(existingDescription.equals(gefaehrdungDescription)){
                            LOG.debug("<" + id + ">\tDuplicate found:\t" + gefaehrdungDescription);
                        }
                    } else {
                        LOG.debug("<" + id + ">\tnew desc for existant g:\t" + gefaehrdungDescription);
                        tMap.put(id, gefaehrdungDescription);
                    }
                } else {
                    LOG.error("Unexpected Class of o:\t" + o.getClass().getCanonicalName());
                }
            }
        }
    }
	
	public List<MbBaustGefaehr> findGefaehrdungenForBaustein(MbBaust mbBaust, NZielobjekt zo){
        BaseHibernateDAO dao = new BaseHibernateDAO();
        Transaction transaction = dao.getSession().beginTransaction();
        Query qry = dao.getSession().createQuery(QUERY_MBBSTGEF_FOR_BAUSTEIN);
        qry.setParameter("bstId", mbBaust.getId().getBauId());
        qry.setParameter("zobId", zo.getId().getZobId());
        List<Object> hqlResult = qry.list();
        transaction.commit();
        dao.getSession().close();
        if(hqlResult != null && hqlResult.size() > 0){
            return (List<MbBaustGefaehr>)(List<?>)hqlResult;
        } else {
            return Collections.EMPTY_LIST;
        }
	}

	public Set<NZielobjekt> findVerantowrtlicheMitarbeiterForMassnahme(
			ModZobjBstMassId id) {
		Set<NZielobjekt> result = new HashSet<NZielobjekt>();
		NZielobjektDAO dao = new NZielobjektDAO();
		Transaction transaction = dao.getSession().beginTransaction();
		Query query = dao.getSession().createQuery(
				QUERY_MITARBEITER_FOR_MASSNAHME);
		query.setProperties(id);
		Iterator iterate = query.iterate();
		while (iterate.hasNext()) {
			result.add((NZielobjekt) iterate.next());
		}
		transaction.commit();
		dao.getSession().close();
		return result;
	}
	
	
	
	public void attachFile(String databaseName, String fileName, String url, String user, String pass) throws SQLException, ClassNotFoundException {
		Class.forName("net.sourceforge.jtds.jdbc.Driver"); //$NON-NLS-1$
		Connection con = DriverManager.getConnection(
				url, user,
				pass);
		Statement stmt = con.createStatement();
		try {
			stmt
			.execute("sp_attach_single_file_db " +
					"@dbname= N\'" + databaseName + "\', " +
					"@physname= N\'"+ fileName + "\'"); //$NON-NLS-1$
		} catch (Exception e) {
			try {
				// if database is attached, try to drop and attach again:
				stmt.execute("sp_detach_db \'" + databaseName + "\'");
			} catch (Exception e2) {
				// do nothing
			}
			stmt
			.execute("sp_attach_single_file_db " +
					"@dbname= N\'" + databaseName + "\', " +
					"@physname= N\'"+ fileName + "\'"); //$NON-NLS-1$
		}
		stmt.close();
		con.close();
	}
	
	public BackupFileLocation getBackupFileNames(String databaseName, String fileName, String url, String user, String pass)
	throws SQLException, ClassNotFoundException {

		BackupFileLocation result = null;
		Class.forName("net.sourceforge.jtds.jdbc.Driver"); //$NON-NLS-1$
		Connection con = DriverManager.getConnection(
				url, user,
				pass);
		Statement stmt = con.createStatement();
		try {
			ResultSet rs = stmt
			.executeQuery("RESTORE FILELISTONLY" +
					 " FROM DISK = '" + fileName + "' ");
			result = new BackupFileLocation();
			if (rs.next()) {
				result.setMdfLogicalName(rs.getString("LogicalName"));
				result.setMdfFileName(rs.getString("PhysicalName"));
			}
			if (rs.next()) {
				result.setLdfLogicalName(rs.getString("LogicalName"));
				result.setLdfFileName(rs.getString("PhysicalName"));
			}
		} catch (Exception e) {
			System.err.println(e);
		}
		stmt.close();
		con.close();
		return result;
	}
	
	public void restoreBackupFile(String databaseName, String fileName, String url, String user, String pass,
			String mdfName, String mdfFile, String ldfName, String ldfFile) 
	throws SQLException, ClassNotFoundException {
		Class.forName("net.sourceforge.jtds.jdbc.Driver"); //$NON-NLS-1$
		Connection con = DriverManager.getConnection(
				url, user,
				pass);
		Statement stmt = con.createStatement();
		try {
			String query = "RESTORE DATABASE " +
			 databaseName + " FROM DISK = '" + fileName + "' " +
			 		"WITH MOVE '" + mdfName +
			 		"' TO '" + mdfFile + 
			 		"', MOVE '" + ldfName +
			 		"' TO '" + ldfFile +
			 		"'"; //$NON-NLS-1$
			stmt
			.execute(query);
		} catch (SQLException e) {
			try {
				stmt.close();
				con.close();
			} catch (Exception e1) {
				// do nothing
			}
			throw e;
		}
		stmt.close();
		con.close();
	}
	
	public Set<NZielobjekt> findBefragteMitarbeiterForBaustein(
			ModZobjBstId id) {
	    // fixme debug this, missing persons for verantwrotlich (mnums) and baustein (befragter)
		Set<NZielobjekt> result = new HashSet<NZielobjekt>();
		NZielobjektDAO dao = new NZielobjektDAO();
		Transaction transaction = dao.getSession().beginTransaction();
		Query query = dao.getSession().createQuery(
				QUERY_MITARBEITER_FOR_BAUSTEIN);
		query.setProperties(id);
		Iterator iterate = query.iterate();
		while (iterate.hasNext()) {
			result.add((NZielobjekt) iterate.next());
		}
		transaction.commit();
		dao.getSession().close();
		return result;
	}

	public List<BausteineMassnahmenResult> findBausteinMassnahmenByZielobjekt(
			NZielobjekt zielobjekt) {
		List<BausteineMassnahmenResult> result = new ArrayList<BausteineMassnahmenResult>();
		NZielobjektDAO dao = new NZielobjektDAO();
		Transaction transaction = dao.getSession().beginTransaction();
		Query query = dao.getSession().createQuery(
				QUERY_BAUSTEIN_ZIELOBJEKT_MASSNAHME_FOR_ZIELOBJEKT);
		query.setProperties(zielobjekt.getId());
		Iterator iterate = query.iterate();
		while (iterate.hasNext()) {
			Object[] next = (Object[]) iterate.next();
			result.add(new BausteineMassnahmenResult((MbBaust) next[0],
					(MbMassn) next[1], (MUmsetzStatTxt) next[2],
					(ModZobjBst) next[3], (ModZobjBstMass) next[4]));
			
			ModZobjBst zobst = (ModZobjBst) next[3];
			if (LOG.isDebugEnabled()) {
			    if (zobst.getRefZobId() != null){
			        LOG.debug("Baustein Referenz: " + zobst.getRefZobId());
			    }
            }
			
		}
		
		transaction.commit();
		dao.getSession().close();
		return result;
	}

	public List<MbRolleTxt> findRollenByZielobjekt(NZielobjekt zielobjekt) {
		List<MbRolleTxt> result = new ArrayList<MbRolleTxt>();
		NZielobjektDAO dao = new NZielobjektDAO();
		Transaction transaction = dao.getSession().beginTransaction();
		Query query = dao.getSession().createQuery(QUERY_ROLLE_FOR_MITARBEITER);
		query.setProperties(zielobjekt.getId());
		Iterator iterate = query.iterate();
		while (iterate.hasNext()) {
			result.add((MbRolleTxt) iterate.next());
		}
		transaction.commit();
		dao.getSession().close();
		return result;
	}

	public List<NZielobjekt> findLinksByZielobjekt(NZielobjekt zielobjekt) {
		List<NZielobjekt> result = new ArrayList<NZielobjekt>();
		NZielobjektDAO dao = new NZielobjektDAO();
		Transaction transaction = dao.getSession().beginTransaction();
		Query query = dao.getSession().createQuery(QUERY_LINKS_FOR_ZIELOBJEKT);
		query.setProperties(zielobjekt.getId());
		Iterator iterate = query.iterate();
		while (iterate.hasNext()) {
			result.add((NZielobjekt) iterate.next());
		}
		transaction.commit();
		dao.getSession().close();
		return result;
	}

	public List<NZobSb> findSchutzbedarfByZielobjekt(NZielobjekt zielobjekt) {
		List<NZobSb> result = new ArrayList<NZobSb>();
		NZobSbDAO dao = new NZobSbDAO();
		Transaction transaction = dao.getSession().beginTransaction();
		Query query = dao.getSession().createQuery(
				QUERY_SCHUTZBEDARF_FOR_ZIELOBJEKT);
		query.setProperties(zielobjekt.getId());
		Iterator iterate = query.iterate();
		while (iterate.hasNext()) {
			result.add((NZobSb) iterate.next());
		}
		transaction.commit();
		dao.getSession().close();
		return result;
	}

	public MSchutzbedarfkategTxt findSchutzbedarfNameForId(Short zsbVerfuSbkId) {
		if (this.allSchutzbedarf == null) {
			allSchutzbedarf = findSchutzbedarfAll();
		}

		for (MSchutzbedarfkategTxt kateg : allSchutzbedarf) {
			if (kateg.getId().getSprId() == 1
					&& kateg.getId().getSbkId().equals(zsbVerfuSbkId))
				return kateg;
		}
		return null;
	}

	public List<String[]> findSubtypesAll() {
		List<String[]> result = new ArrayList<String[]>();
		NZielobjektDAO dao = new NZielobjektDAO();
		Transaction transaction = dao.getSession().beginTransaction();
		Query query = dao.getSession().createQuery(QUERY_ALLSUBTYPES);
		Iterator iterate = query.iterate();
		while (iterate.hasNext()) {
			Object[] next = (Object[]) iterate.next();
			result.add(new String[] { (String) next[0], (String) next[1] });
		}
		transaction.commit();
		dao.getSession().close();
		return result;
	}

	/** 
	 * Joins ESA table to get values for "Ergänzende Sicherheitsanalyse" for a "Zielobjekt"
	 * 
	 * @param zielobjekt
	 * @return
	 */
	public List<ESAResult> findESAByZielobjekt(NZielobjekt zielobjekt) {
		List result = new ArrayList();
		NZielobjektDAO dao = new NZielobjektDAO();
		Transaction transaction = dao.getSession().beginTransaction();
		Query query = dao.getSession().createQuery(
				QUERY_ESA_FOR_ZIELOBJEKT);
		query.setProperties(zielobjekt.getId());
		Iterator iterate = query.iterate();
		while (iterate.hasNext()) {
			result.add((ESAResult) iterate.next());
		}
		transaction.commit();
		dao.getSession().close();
		return result;
	}

	/**
	 * Finds all "Gefährdungen" of "Risikoanalyse" for a "Zielobjekt".
	 * The risk treatment option can be A,B,C,D. 
	 * Gefaehrdungen with option "D" may have additional "massnahmen" linked to them. They have to be loaded separately using
	 * the method <code>findRAGefaehrdungsMassnahmenForZielobjekt()</code> for each gefaehrdung.
	 * 
	 * @param zielobjekt
	 * @return
	 */
	public List<RAGefaehrdungenResult> findRAGefaehrdungenForZielobjekt(NZielobjekt zielobjekt) {
		List result = new ArrayList();
		NZielobjektDAO dao = new NZielobjektDAO();
		Transaction transaction = dao.getSession().beginTransaction();
		Query query = dao.getSession().createQuery(QUERY_RA_GEFS_FOR_ZIELOBJEKT);
		query.setProperties(zielobjekt.getId());
		result.addAll(query.list());
		transaction.commit();
		dao.getSession().close();
		return result;
	
	}

	/**
	 * Loads all "massnahmen" linked to a "gefaehrdung" of a "zielobjekt".
	 * 
	 * 
	 * @param zielobjekt
	 * @param gefaehrdung
	 * @return
	 */
	public List<RAGefaehrdungsMassnahmenResult> findRAGefaehrdungsMassnahmenForZielobjekt(NZielobjekt zielobjekt, MbGefaehr gefaehrdung) {
		List result = new ArrayList();
		NZielobjektDAO dao = new NZielobjektDAO();
		Transaction transaction = dao.getSession().beginTransaction();
		Query query = dao.getSession().createQuery(QUERY_RA_GEF_MNS_FOR_ZIELOBJEKT);
		query.setParameter("zobId", zielobjekt.getId().getZobId());
		query.setParameter("gefId", gefaehrdung.getId().getGefId());
		result.addAll(query.list());
		transaction.commit();
		dao.getSession().close();
		return result;
	}
	
	public static String convertClobToStringEncodingSave(Clob clob, String encoding) throws IOException{
	    try{
	        Reader reader = clob.getCharacterStream();
	        InputStream in = new ByteArrayInputStream(IOUtils.toByteArray(reader, encoding));
	        return IOUtils.toString(in, encoding);
	    } catch (Exception e){
	        LOG.error("Error while converting clob to String", e);
	        throw new RuntimeException(e);
	    }
    }
	
}
